(ns cavia.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [pandect.core :refer [sha1-file]]
            [cavia.common :refer :all]
            [cavia.downloader :as dl]
            [cavia.util :as util]))

(def skeleton-profile {:download-to ".cavia"})

(defmacro defprofile
  "Defines a cavia profile.
  e.g.:
    (defprofile prof
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
      :download-to \".cavia\"})"
  [name profile]
  `(let [profile# (merge skeleton-profile ~profile)]
     (def ~name (with-meta profile# {:tag ::Profile}))))

(def ^:private ^:dynamic *tacit-profile* nil)

(defmacro with-profile
  [profile & body]
  `(binding [*tacit-profile* ~profile]
     ~@body))

(defmacro without-print
  "Restrains printing log, progress, and other messages. Take care that it does
  not restrain error and warning messages."
  [& body]
  `(binding [*verbose* false]
     ~@body))

(defn- meta-tag
  [& args]
  (:tag (meta (first args))))

;;;
;;; Resource path
;;;

(defn- resource*
  [profile id]
  (let [{:keys [resources download-to]} profile]
    (if-let [id* (->> resources
                      (filter #(= (:id %) id))
                      (first)
                      (:id))]
      (fs/absolute-path (str download-to "/" (name id*))))))

(defn- resource-download [profile id]
  (format "%s.download" (resource* profile id)))

(defn- resource-unverified [profile id]
  (format "%s.unverified" (resource* profile id)))

(defmulti resource
  "Returns the local path of the specified resource. Returns nil if the resource
  is not defined in your defcavia. Take care that this function will return the
  path even if the defiend resource is not downloaded."
  meta-tag)

(defmethod resource ::Profile
  [profile id]
  (resource* profile id))

(defmethod resource :default
  [id]
  (resource* *tacit-profile* id))

;;;
;;; Resource info
;;;

(defn- resource-info*
  [profile id]
  (->> (:resources profile)
       (filter #(= (:id %) id))
       (first)))

(defmulti resource-info meta-tag)

(defmethod resource-info ::Profile
  [profile id]
  (resource-info* profile id))

(defmethod resource-info :default
  [id]
  (resource-info* *tacit-profile* id))

;;;
;;; Existence
;;;

(defn- exist?*
  [profile id]
  (let [f (resource profile id)]
    (and (not (nil? f)) (fs/file? f))))

(defn- exist-unverified?
  [profile id]
  (let [f (resource-unverified profile id)]
    (and (not (nil? f)) (fs/file? f))))

(defmulti exist?
  "Returns true if the specified resource exists on local."
  meta-tag)

(defmethod exist? ::Profile
  [profile id]
  (exist?* profile id))

(defmethod exist? :default
  [id]
  (exist?* *tacit-profile* id))

;;;
;;; Validation
;;;

(defn- print-hash-alert
  ([profile id]
     (let [sha1 (->> (:resources profile)
                     (filter #(= (:id %) id))
                     (first)
                     (:sha1))
           f (resource* id)]
       (print-hash-alert id sha1 (sha1-file f))))
  ([id expect-hash actual-hash]
     (println (str "Invalid hash: " id))
     (println (str "  Expected: " expect-hash))
     (println (str "  Actual: " actual-hash))))

(defn- print-missing-alert
  [id]
  (println (str "Missing: " id)))

(defn- valid?*
  [profile id]
  (let [sha1 (->> (:resources profile)
                  (filter #(= (:id %) id))
                  (first)
                  (:sha1))
        f (resource profile id)]
    (and (exist? profile id) (= (sha1-file f) sha1))))

(defn- valid-unverified?
  [profile id]
  (let [sha1 (->> (:resources profile)
                  (filter #(= (:id %) id))
                  (first)
                  (:sha1))
        f (resource-unverified profile id)]
    (and (exist-unverified? profile id) (= (sha1-file f) sha1))))

(defmulti valid?
  "Returns true if the resource's real hash is same as the defined hash."
  meta-tag)

(defmethod valid? ::Profile
  [profile id]
  (valid?* profile id))

(defmethod valid? :default
  [id]
  (valid?* *tacit-profile* id))

;;;
;;; Verification
;;;

(defn- verify*
  ([profile]
     (doseq [{:keys [id]} (:resources profile)]
       (verify* profile id)))
  ([profile id]
     (if (exist? profile id)
       (when-not (valid? profile id)
         (print-hash-alert profile id))
       (print-missing-alert id))))

(defmulti verify
  "Checks existence and hash of the downloaded resource, printing alert message
  when the resource does not exist or the hash is invalid."
  meta-tag)

(defmethod verify ::Profile
  [& args]
  (apply verify* args))

(defmethod verify :default
  [& args]
  (apply (partial verify* *tacit-profile*) args))

;;;
;;; Clean
;;;

(defn- clean!*
  ([profile]
     (fs/delete-dir (:download-to profile))
     nil)
  ([profile id]
     (fs/delete (resource id))
     nil))

(defmulti clean!
  "Removes the specified resource or the download directory."
  meta-tag)

(defmethod clean! ::Profile
  [& args]
  (apply clean!* args))

(defmethod clean! :default
  [& args]
  (apply (partial clean!* *tacit-profile*) args))

;;;
;;; Download
;;;

(defn- get-resource
  [profile id]
  (let [f    (resource profile id)
        dl-f (resource-download profile id)
        uv-f (resource-unverified profile id)
        {:keys [url sha1 auth]} (resource-info profile id)]
    (when *verbose*
      (println (format "Retrieving %s from %s" id url)))
    (condp #(%1 %2) (util/protocol-of url)
      #{:http :https} (dl/http-download! url dl-f :auth auth)
      #{:ftp}         (dl/ftp-download! url dl-f :auth auth)
      (throw (java.net.MalformedURLException. "Unsupported protocol")))
    (fs/rename dl-f uv-f)
    (let [act-sha1 (sha1-file uv-f)]
      (if (= act-sha1 sha1)
        (fs/rename uv-f f)
        (print-hash-alert id sha1 act-sha1)))))

(defn- get!*
  [profile]
  (let [{:keys [resources download-to]} profile]
    (when-not (fs/directory? download-to)
      (fs/mkdir download-to))
    (doseq [r resources]
      (let [id (:id r)]
       (cond
        (valid? profile id) (when *verbose*
                              (println (str "Already downloaded: " id)))
        (valid-unverified? profile id) (do
                                         (fs/rename (resource-unverified profile id)
                                                    (resource profile id))
                                         (when *verbose*
                                           (println (str "Verified " id))))
        :else (get-resource profile id))))))

(defmulti get!
  "Downloads missing resources to the local directory."
  meta-tag)

(defmethod get! ::Profile
  [profile]
  (get!* profile))

(defmethod get! :default
  []
  (get!* *tacit-profile*))
