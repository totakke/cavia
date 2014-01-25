(ns cavy.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [pandect.core :refer [sha1-file]]
            [cavy.common :refer :all]
            [cavy.downloader :as dl]
            [cavy.util :as util]))

;;;
;;; Profile
;;;

(def default-profile {:download-to ".cavy"})

(defonce ^:private cavy-profile (atom nil))

(defn set-profile [profile]
  (reset! cavy-profile profile))

(defmacro defcavy
  "Defines a cavy profile. The last defcavy will be used on all cavy functions.
  e.g.:
    (defcavy mycavy
      {:resources [{:id :resource1
                    :url \"http://example.com/resource1\"
                    :sha1 \"1234567890abcdefghijklmnopqrstuvwxyz1234\"}
                   {:id :resource2
                    :url \"http://example.com/resource2\"
                    :sha1 \"234567890abcdefghijklmnopqrstuvwxyz12345\"
                    :auth {:type :basic, :user \"user\", :password \"password\"}}
                   {:id :resource3
                    :url \"ftp://example.com/resource3\"
                    :sha1 \"34567890abcdefghijklmnopqrstuvwxyz123456\"
                    :auth {:user \"user\", :password \"password\"}}]
      :download-to \".cavy\"})"
  [name profile]
  `(let [profile# (merge default-profile ~profile)]
     (def ~name profile#)
     (set-profile ~name)))

;;;
;;; Verbosity
;;;

(defmacro without-print
  "Restrains printing log, progress, and other messages. Take care that it does
  not restrain error and warning messages."
  [& body]
  `(binding [*verbose* false]
     ~@body))

;;;
;;; Access
;;;

(defn resource
  "Returns the local path of the specified resource. Returns nil if the resource
  is not defined in your defcavy. Take care that this function will return the
  path even if the defiend resource is not downloaded."
  [id]
  (let [{:keys [resources download-to]} @cavy-profile]
    (if-let [id* (->> resources
                      (filter #(= (:id %) id))
                      (first)
                      (:id))]
      (fs/absolute-path (str download-to "/" (name id*))))))

(defn- resource-download [id]
  (str (resource id) ".download"))

(defn- resource-unverified [id]
  (str (resource id) ".unverified"))

(defn exist?
  "Returns true if the specified resource exists on local."
  [id]
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
     (println (str "  Actual: " actual-hash))))

(defn- print-missing-alert
  [id]
  (println (str "Missing: " id)))

(defn valid?
  "Returns true if the resource's real hash is same as the defined hash. "
  [id]
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
  "Checks existence and hash of the downloaded resource, printing alert message
  when the resource does not exist or the hash is invalid."
  ([]
     (doseq [{:keys [id]} (:resources @cavy-profile)]
       (verify id)))
  ([id]
     (if (exist? id)
       (when-not (valid? id)
         (print-hash-alert id))
       (print-missing-alert id))))

;;;
;;; Clean
;;;

(defn clean!
  "Removes the specified resource or the download directory."
  ([] (let [{:keys [download-to]} @cavy-profile]
        (fs/delete-dir download-to))
     nil)
  ([id] (fs/delete (resource id))
     nil))

(defn ^:deprecated clean
  ([] (clean!))
  ([id] (clean! id)))

;;;
;;; Download
;;;

(defn- get* [{:keys [id url sha1 auth]} download-to]
  (let [f (resource id)
        dl-f (resource-download id)
        uv-f (resource-unverified id)]
    (when *verbose*
      (println (str "Retrieving " id " from " url)))
    (condp #(%1 %2) (util/protocol-of url)
      #{:http :https} (dl/http-download! url dl-f :auth auth)
      #{:ftp} (dl/ftp-download! url dl-f :auth auth)
      (throw (java.net.MalformedURLException. "Unsupported protocol")))
    (fs/rename dl-f uv-f)
    (let [act-sha1 (sha1-file uv-f)]
      (if (= act-sha1 sha1)
        (fs/rename uv-f f)
        (print-hash-alert id sha1 act-sha1)))))

(defn get!
  "Downloads missing resources to the local directory."
  []
  (let [{:keys [resources download-to]} @cavy-profile]
    (when-not (fs/directory? download-to)
      (fs/mkdir download-to))
    (doseq [r resources]
      (let [id (:id r)]
       (cond
        (valid? id) (when *verbose*
                      (println (str "Already downloaded: " id)))
        (valid-unverified? id) (do (fs/rename (resource-unverified id) (resource id))
                                   (when *verbose*
                                     (println (str "Verified " id))))
        :else (get* r download-to))))))

(defn ^:deprecated get
  []
  (get!))
