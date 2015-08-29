(ns cavia.downloader
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-http.lite.client :as client]
            [cemerick.url :as c-url]
            [cavia.common :refer :all])
  (:import [java.io InputStream OutputStream]
           java.net.URLDecoder
           [org.apache.commons.net.ftp FTP FTPClient FTPSClient FTPReply]))

(def ^:private printed-percentage (atom -1))

(defn- print-progress
  [now total]
  (let [percentage (quot (* now 100) total)]
    (when-not (= percentage @printed-percentage)
      (print (str "\r"
                  (string/join
                   (map-indexed (fn [idx _]
                                  (if (< idx (quot percentage 2)) \# \space))
                                (repeat 50 nil)))
                  "| " percentage "%"))
      (flush)
      (reset! printed-percentage percentage))))

(defn- download!
  "Downloads from the InputStream to the OutputStream. To print progress, it
  requires the content length."
  [^InputStream is ^OutputStream os content-len]
  (let [data (byte-array *download-buffer-size*)]
    (loop [len (.read is data)
           sum len]
      (when-not (= len -1)
        (when (and *verbose* (pos? content-len))
          (print-progress sum content-len))
        (.write os data 0 len)
        (let [len (.read is data)]
          (recur len (+ sum len))))))
  (reset! printed-percentage -1)
  (when (and *verbose* (pos? content-len))
    (newline)))

(defn http-download!
  "Downloads from the url via HTTP/HTTPS and saves it to local as f."
  [url f & {:keys [auth]}]
  (let [option (merge {:as :stream}
                      (if-let [{:keys [type user password]} auth]
                        {(keyword (str (name type) "-auth")) [user password]}))
        response (client/get url option)
        content-len (if-let [content-len (get-in response [:headers "content-length"])]
                      (Integer. ^String content-len) -1)
        is (:body response)]
    (with-open [os (io/output-stream f)]
      (download! is os content-len))))

(defn- ftp-client
  [url]
  (let [u (c-url/url url)
        ^FTPClient client (case (:protocol u)
                            "ftp" (FTPClient.)
                            "ftps" (FTPSClient.)
                            (throw (Exception. (str "unexpected protocol "
                                                    (:protocol u)
                                                    " in FTP url, need \"ftp\" or \"ftps\""))))]
    (.connect client (:host u) (if (= -1 (:port u)) 21 (:port u)))
    (let [reply (.getReplyCode client)]
      (if (not (FTPReply/isPositiveCompletion reply))
        (do (.disconnect client)
            (println "Connection refused")
            nil)
        client))))

(defn- url-decode
  [s]
  (if s (URLDecoder/decode s "UTF-8") ""))

(defn- complete-pending-command
  [ftp-client]
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
            content-len (.. client* (mlistFile (:path u)) getSize)]
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
