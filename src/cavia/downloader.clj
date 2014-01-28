(ns cavia.downloader
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [miner.ftp :as ftp]
            [clojurewerkz.urly.core :as urly]
            [cavia.common :refer :all])
  (:import [java.io InputStream OutputStream]))

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
        (when *verbose*
          (print-progress sum content-len))
        (.write os data 0 len)
        (let [len (.read is data)]
          (recur len (+ sum len))))))
  (reset! printed-percentage -1)
  (when *verbose*
    (newline)))

(defn http-download!
  "Downloads from the url via HTTP/HTTPS and saves it to local as f."
  [url f & {:keys [auth]}]
  (let [option (merge {:as :stream}
                      (if-let [{:keys [type user password]} auth]
                        {(keyword (str (name type) "-auth")) [user password]}))
        response (client/get url option)
        content-len (Integer. ^String (get-in response [:headers "content-length"]))
        is (:body response)]
    (with-open [os (io/output-stream f)]
      (download! is os content-len))))

(defn ftp-download!
  "Downloads from the url via FTP and saves it to local as f."
  [url f & {:keys [auth]}]
  (let [u (urly/url-like url)
        host (str (urly/protocol-of u) "://"
                  (if-let [{:keys [user password]} auth]
                    (str user ":" password "@" (urly/host-of u))
                    (urly/authority-of u)))
        path (urly/path-of u)]
    (ftp/with-ftp [ftp-client host :file-type :binary]
      (.setSoTimeout ftp-client 30000)
      (.setDataTimeout ftp-client 30000)
      (let [content-len (.. ftp-client (mlistFile path) getSize)
            is (ftp/client-get-stream ftp-client path)]
        (with-open [os (io/output-stream f)]
          (download! is os content-len))
        (.close is)
        (try
          (ftp/client-complete-pending-command ftp-client)
          (catch java.net.SocketTimeoutException e
            ;; NOTE: `client-complete-pending-command` sometimes hangs after
            ;;       downloading a large file. But the file is fine and the
            ;;       downloading process succeded to finish. Therefore here
            ;;       ignores the timeout.
            nil))))))
