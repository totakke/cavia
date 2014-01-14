(ns cavy.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [pandect.core :refer [sha1-file]]))

(def default-profile {:download-to ".cavy"})

(defonce ^:private cavy-profile (atom nil))

(defn set-profile [profile]
  (reset! cavy-profile profile))

(defmacro defcavy
  [name profile]
  `(let [profile# (merge default-profile ~profile)]
     (def ~name profile#)
     (set-profile ~name)))

(defn- print-hash-alert
  [id expect-hash actual-hash]
  (println "Invalid hash: " id)
  (println "  Expected: " expect-hash)
  (println "    Actual: " actual-hash))

(defn resource [id]
  (let [{:keys [resources download-to]} @cavy-profile]
    (if-let [r (first (filter #(= (:id %) id) resources))]
      (str download-to "/" (:id r)))))

(defn exist? [id]
  (let [f (resource id)]
    (and (not (nil? f)) (fs/file? f))))

(defn valid? [id]
  (let [sha1 (->> (:resources @cavy-profile)
                  (filter #(= (:id %) id))
                  (first)
                  (:sha1))
        f (resource id)]
    (and (exist? id) (= (sha1-file f) sha1))))

(defn verify []
  (let [{:keys [resources download-to]} @cavy-profile]
    (doseq [{:keys [id sha1]} resources]
      (let [act-sha1 (sha1-file (str download-to "/" id))]
        (when-not (= act-sha1 sha1)
          (print-hash-alert id sha1 act-sha1))))))

(defn clean []
  (let [{:keys [download-to]} @cavy-profile]
    (fs/delete-dir download-to))
  nil)

(defn- download
  [url f]
  (with-open [in (io/input-stream url)
              out (io/output-stream f)]
    (io/copy in out)))

(defn- get* [resource download-to]
  (let [{:keys [id url sha1]} resource
        f (str download-to "/" id)
        download-f (str f ".download")
        unverified-f (str f ".unverified")]
    (println (str "Retrieving " id " from " url))
    (download url download-f)
    (fs/rename download-f unverified-f)
    (let [act-sha1 (sha1-file unverified-f)]
      (if (= act-sha1 sha1)
        (fs/rename unverified-f f)
        (print-hash-alert id sha1 act-sha1)))))

(defn get []
  (let [{:keys [resources download-to]} @cavy-profile]
    (when-not (fs/directory? download-to)
      (fs/mkdir download-to))
    (doseq [r resources]
      (if (valid? (:id r))
        (println "Already downloaded: " (:id r))
        (get* r download-to)))))
