(ns cavia.util)

(defn str->int [s]
  (if-not (nil? s)
    (try
      (let [[n _ _] (re-matches #"(|-|\+)(\d+)" s)]
        (Integer. ^String n))
      (catch NumberFormatException e
        (Long. ^String s)))))
