(ns cavy.test-util
  (:require [clojure.java.io :as io]))

(defmacro with-out-null
  [& body]
  `(binding [*out* (io/writer "/dev/null")]
     ~@body))

(def temp-dir (.getPath (io/file (System/getProperty "java.io.tmpdir") "cavy-test")))

(defn prepare-cache! []
  (.mkdir (io/file temp-dir)))

(defn clean-cache! []
  (let [dir (io/file temp-dir)]
    (when (.exists dir)
      (doseq [f (seq (.list dir))]
        (.delete (io/file (str temp-dir "/" f))))
      (.delete dir))))
