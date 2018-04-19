(ns cavia.internal
  (:require [clojure.java.io :as io]))

(defn str->int [s]
  (if-not (nil? s)
    (try
      (let [[n _ _] (re-matches #"(|-|\+)(\d+)" s)]
        (Integer. ^String n))
      (catch NumberFormatException e
        (Long. ^String s)))))

(defn delete-dir
  [root]
  (let [f (io/file root)]
    (when (.isDirectory f)
      (doseq [path (.listFiles f)]
        (delete-dir path)))
    (if (.exists f) (io/delete-file root))))
