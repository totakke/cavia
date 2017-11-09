(ns cavia.downloader
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [cemerick.url :as c-url]
            [progrock.core :as pr]
            [cavia.common :refer :all]
            [cavia.util :refer [str->int]])
  (:import [java.io InputStream OutputStream IOException]
           java.net.URLDecoder
           [org.apache.commons.net.ftp FTP FTPClient FTPSClient FTPReply]))

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

(defn- ^FTPClient ftp-client
  [url]
  (let [u (c-url/url url)
        ^FTPClient client (case (:protocol u)
                            "ftp" (FTPClient.)
                            "ftps" (FTPSClient.)
                            (throw (Exception. (str "unexpected protocol "
                                                    (:protocol u)
                                                    " in FTP url, need \"ftp\" or \"ftps\""))))]
    (.connect client ^String (:host u) (int (if (= -1 (:port u)) 21 (:port u))))
    (let [reply (.getReplyCode client)]
      (if-not (FTPReply/isPositiveCompletion reply)
        (do (.disconnect client)
            (binding [*out* *err*]
              (println "Connection refused"))
            nil)
        client))))

(defn- url-decode
  [s]
  (if s (URLDecoder/decode s "UTF-8") ""))

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
  (when-let [client* (ftp-client url)]
    (try
      (when-let [{:keys [user password]} auth]
        (.login client* (url-decode user) (url-decode password)))
      (doto client*
        (.setFileType FTP/BINARY_FILE_TYPE)
        (.setControlKeepAliveTimeout 300)
        (.setControlKeepAliveReplyTimeout 1000)
        (.setSoTimeout 30000)
        (.setDataTimeout 30000)
        (.enterLocalPassiveMode))
      (let [u (c-url/url url)
            content-len (try (.. client* (mlistFile (:path u)) getSize) (catch IOException _ -1))]
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
          nil))
      (finally (when (.isConnected client*)
                 (.disconnect client*))))))
