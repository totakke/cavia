(ns cavy.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [pandect.core :refer [sha1-file]]))

(def ^:private default-profile {:download-to ".cavy"})

(defonce ^:private cavy-profile (atom nil))

(defn set-profile [profile]
  (reset! cavy-profile (merge profile default-profile)))

(defmacro defcavy
  [name spec]
  `(let [spec# ~spec]
     (def ~name spec#)
     (set-profile ~name)))

(defn- print-hash-alert
  [id expect-hash actual-hash]
  (println "Invalid hash: " id)
  (println "  Expected: " expect-hash)
  (println "    Actual: " actual-hash))

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
      (get* r download-to))))

(defn resource [id]
  (let [{:keys [resources download-to]} @cavy-profile]
    (if-let [r (first (filter #(= (:id %) id) resources))]
      (str download-to "/" (:id r))
      nil)))
