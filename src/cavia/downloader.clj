(ns cavia.downloader
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [lambdaisland.uri :as uri]
            [progrock.core :as pr]
            [cavia.common :refer :all]
            [cavia.ftp :as ftp]
            [cavia.internal :refer [str->int]]
            [cavia.sftp :as sftp]
            [cavia.util :as util])
  (:import (java.io InputStream OutputStream IOException)
           (com.jcraft.jsch ChannelSftp$LsEntry)
           (org.apache.commons.net.ftp FTPClient FTPConnectionClosedException
                                       FTPReply)
           (org.apache.http ConnectionClosedException)))

(defn- download!
  "Downloads from the InputStream to the OutputStream. To print progress, it
  requires the content length."
  [^InputStream is ^OutputStream os content-len resume]
  (let [data (byte-array *download-buffer-size*)
        with-print (and *verbose* (pos? content-len))
        resume (or resume 0)]
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
            (recur len (+ sum len) (assoc bar :progress (quot (* sum 100) content-len)))))))))

(def default-max-retries 3)

(defn- http-download!*
  [url f nretry max-retries {:keys [auth resume] :as opts}]
  (try
    (let [file (io/file f)
          resume (when (.exists file)
                   (if (true? resume) (.length file) resume))
          option (merge {:as :stream}
                        (if-let [{:keys [type user password]} auth]
                          {(keyword (str (name type) "-auth")) [user password]})
                        (when resume
                          {:headers {:range (str "bytes=" resume "-")}}))
          response (client/get url option)
          content-len (if-let [content-len (get-in response [:headers "content-length"])]
                        (str->int content-len) -1)
          is (:body response)]
      (with-open [os (io/output-stream file :append (boolean resume))]
        (download! is os content-len resume)))
    (catch ConnectionClosedException e
      (if (<= nretry max-retries)
        (http-download!* url f (inc nretry) max-retries opts)
        (throw e)))))

(defn http-download!
  "Downloads from the url via HTTP/HTTPS and saves it to local as f.

  Options:

    :auth    Username and password for Basic/Digest authentications.
             e.g. {:type :basic, :user \"user\", :password \"password\"}

    :resume  Resume downloading a partially downloaded file if true. You can
             also specify a resuming byte position as an integer.

    :retry   Retry downloading when a network error occurs if true. You can
             specify a maximum retry count as an integer (default: 3). When this
             option is enabled, :resume option will be automatically set to
             true."
  [url f & {:keys [retry] :as opts}]
  (let [max-retries (cond
                      (integer? retry) retry
                      (true? retry) default-max-retries
                      :else 0)
        opts (cond-> (dissoc opts :retry)
               (pos? max-retries) (assoc :resume true))]
    (http-download!* url f 0 max-retries opts)))

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

(defn ftp-download!*
  [url f nretry max-retries {:keys [auth resume] :as opts}]
  (try
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
        (catch java.net.SocketTimeoutException e
          ;; NOTE: `client-complete-pending-command` sometimes hangs after
          ;;       downloading a large file. But the file is fine and the
          ;;       downloading process succeded to finish. Therefore here
          ;;       ignores the timeout.
          nil)))
    (catch FTPConnectionClosedException e
      (if (<= nretry max-retries)
        (ftp-download!* url f (inc nretry) max-retries opts)
        (throw e)))))

(defn ftp-download!
  "Downloads from the url via FTP and saves it to local as f.

  Options:

    :auth    Username and password for FTP authentication.
             e.g. {:user \"user\", :password \"password\"}

    :resume  Resume downloading a partially downloaded file if true. You can
             also specify a resuming byte position as an integer.

    :retry   Retry downloading when a network error occurs if true. You can
             specify a maximum retry count as an integer (default: 3). When this
             option is enabled, :resume option will be automatically set to
             true."
  [url f & {:keys [retry] :as opts}]
  (let [max-retries (cond
                      (integer? retry) retry
                      (true? retry) default-max-retries
                      :else 0)
        opts (cond-> (dissoc opts :retry)
               (pos? max-retries) (assoc :resume true))]
    (ftp-download!* url f 0 max-retries opts)))

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
