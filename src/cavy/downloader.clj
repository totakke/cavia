(ns cavy.downloader
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [miner.ftp :as ftp]
            [clojurewerkz.urly.core :as urly]
            [cavy.common :refer :all])
  (:import java.io.FilterInputStream))

(defn- print-progress
  [now total]
  (let [percentage (quot (* now 100) total)]
    (print (str "\r"
                (string/join
                 (map-indexed (fn [idx _]
                                (if (< idx (quot percentage (quot 100 50))) \# \space))
                              (repeat 50 nil)))
                "| " percentage "%"))))

(defn http-download!
  [url f & {:keys [auth]}]
  (let [option (merge {:as :stream}
                      (if-let [{:keys [type user password]} auth]
                        {(keyword (str (name type) "-auth")) [user password]}))
        response (client/get url option)
        content-len (Integer. ^String (get-in response [:headers "content-length"]))
        is ^FilterInputStream (:body response)
        data (byte-array 1024)]
    (with-open [w (io/output-stream f)]
      (loop [len (.read is data)
             sum len]
        (when-not (= len -1)
          (when *verbose*
            (print-progress sum content-len))
          (.write w data 0 len)
          (let [len (.read is data)]
            (recur len (+ sum len)))))
      (when *verbose*
        (println)))))

(defn ftp-download!
  [url f & {:keys [auth]}]
  (let [u (urly/url-like url)
        host (str (urly/protocol-of u) "://"
                  (if-let [{:keys [user password]} auth]
                    (str user ":" password "@" (urly/host-of u))
                    (urly/authority-of u)))
        path (urly/path-of u)]
    (ftp/with-ftp [ftp-client host]
      (let [is (ftp/client-get-stream ftp-client path)
            data (byte-array 1024)]
        (with-open [w (io/output-stream f)]
          (loop [len (.read is data)
                 sum len]
            (when-not (= len -1)
              ;; (when *verbose*
              ;;   (print-progress sum content-len))
              (.write w data 0 len)
              (let [len (.read is data)]
                (recur len (+ sum len)))))
          (when *verbose*
            (println)))))))
