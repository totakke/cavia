(ns cavia.downloader
  (:require [cavia.common :refer [*download-buffer-size* *verbosity*]]
            [cavia.ftp :as ftp]
            [cavia.internal :refer [str->int]]
            [cavia.sftp :as sftp]
            [cavia.specs :as specs]
            [cavia.util :as util]
            [clj-http.lite.client :as client]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [lambdaisland.uri :as uri]
            [progrock.core :as pr])
  (:import [java.io File InputStream OutputStream IOException]
           java.net.MalformedURLException
           [com.jcraft.jsch ChannelSftp$LsEntry]
           [org.apache.commons.net.ftp FTPClient FTPReply]))

(defn- download!
  "Downloads from the InputStream to the OutputStream. To print progress, it
  requires the content length."
  [^InputStream is ^OutputStream os ^long content-len resume]
  (let [data (byte-array *download-buffer-size*)
        with-print (and (:download *verbosity*) (pos? content-len))
        ^long resume (or resume 0)
        size (+ content-len resume)]
    (loop [len (.read is data)
           sum (+ resume len)
           bar (pr/progress-bar 100)]
      (when (Thread/interrupted) (throw (InterruptedException.)))
      (if (= len -1)
        (when with-print (pr/print (pr/done bar)))
        (do
          (when with-print (pr/print bar))
          (.write os data 0 len)
          (let [len (.read is data)]
            (recur len (+ sum len) (assoc bar :progress (quot (* sum 100) size)))))))))

(defn http-download!
  "Downloads from the url via HTTP/HTTPS and saves it to local as f.

  Options:

    :auth    Credentials for Basic/Digest/OAuth2 authentications.
             e.g. {:type :basic, :user \"user\", :password \"password\"}
                  {:type :oauth2, :token \"access-token\"}

    :resume  Resume downloading a partially downloaded file if true. You can
             also specify a resuming byte position as an integer."
  [url f & {:keys [auth resume]}]
  (let [file (io/file f)
        resume (when (.exists file)
                 (if (true? resume) (.length file) resume))
        option (merge {:as :stream}
                      (case (:type auth)
                        :basic {:basic-auth [(:user auth) (:password auth)]}
                        :digest {:digest-auth [(:user auth) (:password auth)]}
                        :oauth2 {:oauth-token (:token auth)}
                        nil)
                      (when resume
                        {:headers {"range" (str "bytes=" resume "-")}}))
        response (client/get (str url) option)
        content-len (if-let [content-len (get-in response [:headers "content-length"])]
                      (str->int content-len) -1)
        is (:body response)]
    (with-open [os (io/output-stream file :append (boolean resume))]
      (download! is os content-len resume))))

(s/fdef http-download!
  :args (s/cat :url ::specs/url
               :f (s/or :string string?
                        :file #(instance? File %))
               :opts (s/keys* :opt-un [:cavia.specs.downloader/auth
                                       :cavia.specs.downloader/resume])))

(defn- ftp-content-len
  [^FTPClient ftp-client path]
  (try
    (if-let [file (.mlistFile ftp-client path)]
      (.getSize file)
      -1)
    (catch IOException _
      -1)))

(defn- complete-pending-command
  [^FTPClient ftp-client]
  (.completePendingCommand ftp-client)
  (let [reply-code (.getReplyCode ftp-client)]
    (when-not (FTPReply/isPositiveCompletion reply-code)
      (throw (ex-info "Not a Positive completion of last command"
                      {:reply-code reply-code
                       :reply-string (.getReplyString ftp-client)})))))

(defn ftp-download!
  "Downloads from the url via FTP and saves it to local as f.

  Options:

    :auth    Username and password for FTP authentication.
             e.g. {:user \"user\", :password \"password\"}

    :resume  Resume downloading a partially downloaded file if true. You can
             also specify a resuming byte position as an integer."
  [url f & {:keys [auth resume]}]
  (util/with-connection [client* (ftp/client url {:auth auth})]
    (let [file (io/file f)
          resume (when (.exists file)
                   (if (true? resume) (.length file) resume))
          u (uri/uri url)
          content-len (ftp-content-len client* (:path u))]
      (when resume
        (.setRestartOffset client* resume))
      (with-open [is ^InputStream (.retrieveFileStream client* (:path u))
                  os (io/output-stream file :append (boolean resume))]
        (download! is os content-len resume)))
    (try
      (complete-pending-command client*)
      (catch java.net.SocketTimeoutException _
        ;; NOTE: `client-complete-pending-command` sometimes hangs after
        ;;       downloading a large file. But the file is fine and the
        ;;       downloading process succeded to finish. Therefore here
        ;;       ignores the timeout.
        nil))))

(s/fdef ftp-download!
  :args (s/cat :url ::specs/url
               :f (s/or :string string?
                        :file #(instance? File %))
               :opts (s/keys* :opt-un [:cavia.specs.downloader/auth
                                       :cavia.specs.downloader/resume])))

(defn sftp-download!
  [url f auth]
  (util/with-connection [session (sftp/session url {:auth auth})
                         channel (sftp/channel session)]
    (let [u (uri/uri url)
          [^ChannelSftp$LsEntry entry] (.ls channel (:path u))
          content-len (.. entry getAttrs getSize)]
      (with-open [is (.get channel (:path u))
                  os (io/output-stream f)]
        (download! is os content-len 0)))))

(s/fdef sftp-download!
  :args (s/cat :url ::specs/url
               :f (s/or :string string?
                        :file #(instance? File %))
               :auth ::specs/auth))

(defn- s3-client
  [url auth]
  (let [u (uri/uri url)
        endpoint (when-not (re-find #"s3[-\.][-\da-z]+\.amazonaws\.com" (:host u))
                   {:protocol (:scheme u)
                    :hostname (:host u)
                    :port (str->int (:port u))})
        [_ region] (re-find #"s3[-\.]([-\da-z]+)\.amazonaws\.com" (:host u))
        creds (credentials/basic-credentials-provider auth)]
    (aws/client (cond-> {:api :s3
                         :credentials-provider creds}
                  endpoint (assoc :endpoint-override endpoint)
                  region (assoc :region region)))))

(defn- s3-bucket-key
  [url]
  (let [u (uri/uri url)]
    (if-let [[_ bucket]
             (re-matches
              #"([\da-z][-.\da-z]{2,62})\.s3([-\.][-\da-z]+)?\.amazonaws\.com"
              (:host u))]
      {:bucket bucket, :key (second (re-matches #"/(.+)" (:path u)))}
      (if-let [[_ bucket key] (re-matches #"/([\da-z][-.\da-z]{2,62})/(.+)"
                                          (:path u))]
        {:bucket bucket, :key key}
        (throw (MalformedURLException. "Illegal format of a bucket and a key"))))))

(defn s3-download!
  "Downloads from the url via S3 protocol and saves it to local as f. The auth
  format is {:access-key-id \"accesskey\" :secret-access-key \"secretkey\"}.

  Options:

    :resume  Resume downloading a partially downloaded file if true. You can
             also specify a resuming byte position as an integer."
  [url f auth & {:keys [resume]}]
  (let [s3 (s3-client url auth)
        {:keys [bucket key]} (s3-bucket-key url)
        file (io/file f)
        resume (when (.exists file)
                 (if (true? resume) (.length file) resume))
        range (when resume
                (str "bytes=" resume "-"))
        content-len (:ContentLength
                     (aws/invoke s3 {:op :HeadObject
                                     :request {:Bucket bucket :Key key}}))]
    (with-open [is ^InputStream (:Body
                                 (aws/invoke
                                  s3
                                  {:op :GetObject
                                   :request (cond-> {:Bucket bucket :Key key}
                                              range (assoc :Range range))}))
                os (io/output-stream file :append (boolean resume))]
      (download! is os content-len resume))))

(s/fdef s3-download!
  :args (s/cat :url ::specs/url
               :f (s/or :string string?
                        :file #(instance? File %))
               :auth ::specs/auth
               :opts (s/keys* :opt-un [:cavia.specs.downloader/resume])))
