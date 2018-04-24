(ns cavia.internal
  (:require [clojure.java.io :as io])
  (:import java.net.URLDecoder))

(defn str->int [s]
  (when (some? s)
    (try
      (Integer/parseInt s)
      (catch NumberFormatException _
        (Long/parseLong s)))))

(defn delete-dir
  [root]
  (let [f (io/file root)]
    (when (.isDirectory f)
      (doseq [path (.listFiles f)]
        (delete-dir path)))
    (if (.exists f) (io/delete-file root))))

(defn url-decode
  [^String s]
  (if s (URLDecoder/decode s "UTF-8") ""))
