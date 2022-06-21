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
    (when (.exists f)
      (io/delete-file root))))

(defn url-decode
  [^String s]
  (if s (URLDecoder/decode s "UTF-8") ""))

(defn parse-auth
  [uri auth]
  (cond
    auth (-> auth
             (update :user url-decode)
             (update :password url-decode))
    (or (:user uri) (:password uri)) (select-keys uri [:user :password])))
