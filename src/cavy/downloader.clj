(ns cavy.downloader
  (:require [clojure.string :as string]
            [clj-http.client :as client]
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
    (with-open [w (clojure.java.io/output-stream f)]
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
