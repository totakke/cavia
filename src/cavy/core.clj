(ns cavy.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [pandect.core :refer [sha1-file]]
            [cavy.common :refer :all]
            [cavy.downloader :as dl]))

;;;
;;; Profile
;;;

(def default-profile {:download-to ".cavy"})

(defonce ^:private cavy-profile (atom nil))

(defn set-profile [profile]
  (reset! cavy-profile profile))

(defmacro defcavy
  [name profile]
  `(let [profile# (merge default-profile ~profile)]
     (def ~name profile#)
     (set-profile ~name)))

;;;
;;; Verbosity
;;;

(defmacro without-print [& body]
  `(binding [*verbose* false]
     ~@body))

;;;
;;; Access
;;;

(defn resource [id]
  (let [{:keys [resources download-to]} @cavy-profile]
    (if-let [r (first (filter #(= (:id %) id) resources))]
      (str download-to "/" (:id r)))))

(defn- resource-unverified [id]
  (str (resource id) ".unverified"))

(defn exist? [id]
  (let [f (resource id)]
    (and (not (nil? f)) (fs/file? f))))

(defn- exist-unverified? [id]
  (let [f (resource-unverified id)]
    (and (not (nil? f)) (fs/file? f))))

;;;
;;; Validation
;;;

(defn- print-hash-alert
  ([id]
     (let [sha1 (->> (:resources @cavy-profile)
                     (filter #(= (:id %) id))
                     (first)
                     (:sha1))
           f (resource id)]
       (print-hash-alert id sha1 (sha1-file f))))
  ([id expect-hash actual-hash]
     (println (str "Invalid hash: " id))
     (println (str "  Expected: " expect-hash))
     (println (str "    Actual: " actual-hash))))

(defn valid? [id]
  (let [sha1 (->> (:resources @cavy-profile)
                  (filter #(= (:id %) id))
                  (first)
                  (:sha1))
        f (resource id)]
    (and (exist? id) (= (sha1-file f) sha1))))

(defn- valid-unverified? [id]
  (let [sha1 (->> (:resources @cavy-profile)
                  (filter #(= (:id %) id))
                  (first)
                  (:sha1))
        f (resource-unverified id)]
    (and (exist-unverified? id) (= (sha1-file f) sha1))))

(defn verify
  ([] (let [{:keys [resources download-to]} @cavy-profile]
        (doseq [{:keys [id sha1]} resources]
          (let [act-sha1 (sha1-file (resource id))]
            (when-not (= act-sha1 sha1)
              (print-hash-alert id sha1 act-sha1))))))
  ([id] (if (exist? id)
          (when-not (valid? id)
            (print-hash-alert id))
          (when (valid-unverified? id)
            (fs/rename (resource-unverified id) (resource id))
            (when *verbose*
              (println (str "Verified " id)))))))

;;;
;;; Clean
;;;

(defn clean
  ([] (let [{:keys [download-to]} @cavy-profile]
        (fs/delete-dir download-to))
     nil)
  ([id] (fs/delete (resource id))
     nil))

;;;
;;; Download
;;;

(defn- get* [resource download-to]
  (let [{:keys [id url sha1 auth]} resource
        f (str download-to "/" id)
        download-f (str f ".download")
        unverified-f (str f ".unverified")]
    (when *verbose*
      (println (str "Retrieving " id " from " url)))
    (dl/http-download! url download-f :auth auth)
    (fs/rename download-f unverified-f)
    (let [act-sha1 (sha1-file unverified-f)]
      (if (= act-sha1 sha1)
        (fs/rename unverified-f f)
        (print-hash-alert id sha1 act-sha1)))))

(defn get
  []
  (let [{:keys [resources download-to]} @cavy-profile]
    (when-not (fs/directory? download-to)
      (fs/mkdir download-to))
    (doseq [r resources]
      (cond
       (valid? (:id r)) (when *verbose*
                          (println "Already downloaded: " (:id r)))
       (valid-unverified? (:id r)) (verify (:id r))
       :else (get* r download-to)))))
