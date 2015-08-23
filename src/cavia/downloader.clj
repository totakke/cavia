(ns cavia.downloader
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [miner.ftp :as ftp]
            [cemerick.url :as c-url]
            [progrock.core :as pr]
            [cavia.common :refer :all])
  (:import [java.io InputStream OutputStream]))

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
                      (Integer. ^String content-len) -1)
        is (:body response)]
    (with-open [os (io/output-stream f)]
      (download! is os content-len))))

(defn ftp-download!
  "Downloads from the url via FTP and saves it to local as f."
  [url f & {:keys [auth]}]
  (let [u (c-url/url url)
        root-url (str (:protocol u) "://"
                      (if-let [{:keys [user password]} auth]
                        (str user ":" password "@"))
                      (:host u))
        path (:path u)]
    (ftp/with-ftp [ftp-client root-url :file-type :binary]
      (.setSoTimeout ftp-client 30000)
      (.setDataTimeout ftp-client 30000)
      (let [content-len (.. ftp-client (mlistFile path) getSize)]
        (with-open [is ^InputStream (ftp/client-get-stream ftp-client path)
                    os (io/output-stream f)]
          (download! is os content-len)))
      (try
        (ftp/client-complete-pending-command ftp-client)
        (catch java.net.SocketTimeoutException e
          ;; NOTE: `client-complete-pending-command` sometimes hangs after
          ;;       downloading a large file. But the file is fine and the
          ;;       downloading process succeded to finish. Therefore here
          ;;       ignores the timeout.
          nil)))))
