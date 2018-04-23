(ns cavia.downloader
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [lambdaisland.uri :as uri]
            [progrock.core :as pr]
            [cavia.common :refer :all]
            [cavia.ftp :as ftp]
            [cavia.internal :refer [str->int]])
  (:import [java.io InputStream OutputStream IOException]
           [com.jcraft.jsch ChannelSftp ChannelSftp$LsEntry JSch Session]
           [org.apache.commons.net.ftp FTPClient FTPReply]))

(defn- download!
  "Downloads from the InputStream to the OutputStream. To print progress, it
  requires the content length."
  [^InputStream is ^OutputStream os content-len]
  (let [data (byte-array *download-buffer-size*)
        with-print (and *verbose* (pos? content-len))]
    (loop [len (.read is data)
           sum len
           bar (pr/progress-bar 100)]
      (if (= len -1)
        (when with-print (pr/print (pr/done bar)))
        (do
          (when with-print (pr/print bar))
          (.write os data 0 len)
          (let [len (.read is data)]
            (recur len (+ sum len) (assoc bar :progress (quot (* sum 100) content-len)))))))))

(defn http-download!
  "Downloads from the url via HTTP/HTTPS and saves it to local as f."
  [url f & {:keys [auth]}]
  (let [option (merge {:as :stream}
                      (if-let [{:keys [type user password]} auth]
                        {(keyword (str (name type) "-auth")) [user password]}))
        response (client/get url option)
        content-len (if-let [content-len (get-in response [:headers "content-length"])]
                      (str->int ^String content-len) -1)
        is (:body response)]
    (with-open [os (io/output-stream f)]
      (download! is os content-len))))

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
  "Downloads from the url via FTP and saves it to local as f."
  [url f & {:keys [auth]}]
  (ftp/with-connection [client* (ftp/client url {:auth auth})]
    (let [u (uri/uri url)
          content-len (ftp-content-len client* (:path u))]
      (with-open [is ^InputStream (.retrieveFileStream client* (:path u))
                  os (io/output-stream f)]
        (download! is os content-len)))
    (try
      (complete-pending-command client*)
      (catch java.net.SocketTimeoutException e
        ;; NOTE: `client-complete-pending-command` sometimes hangs after
        ;;       downloading a large file. But the file is fine and the
        ;;       downloading process succeded to finish. Therefore here
        ;;       ignores the timeout.
        nil))))

(defn ^Session sftp-session
  [url auth]
  (let [u (uri/uri url)]
    (doto (.getSession (JSch.) (:user auth) (:host u) (or (str->int (:port u)) 22))
      (.setPassword ^String (:password auth))
      (.setConfig "StrictHostKeyChecking" "no"))))

(defn sftp-download!
  [url f auth]
  (let [u (uri/uri url)
        session (sftp-session url auth)]
    (try
      (.connect session)
      (let [^ChannelSftp channel (.openChannel session "sftp")]
        (try
          (.connect channel)
          (let [[^ChannelSftp$LsEntry entry] (.ls channel (:path u))
                content-len (.. entry getAttrs getSize)]
            (with-open [is (.get channel (:path u))
                        os (io/output-stream f)]
              (download! is os content-len)))
          (finally (.disconnect channel))))
      (finally (.disconnect session)))))
