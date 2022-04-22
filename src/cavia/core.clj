(ns cavia.core
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [digest]
            [lambdaisland.uri :as uri]
            [cavia.common :refer :all]
            [cavia.downloader :as dl]
            [cavia.decompressor :as dc]
            [cavia.internal :refer [delete-dir]]))

(def skeleton-profile {:download-to ".cavia"})

(defmacro defprofile
  "Defines a cavia profile.

  e.g.:

      (defprofile prof
        {:resources [{:id :resource1
                      :url \"http://example.com/resource1\"
                      :sha256 \"0123456789abcdef01234567890abcdef01234567890abcdef01234567890abc\"}
                     {:id :resource2
                      :url \"http://example.com/resource2\"
                      :sha1 \"123456789abcdef01234567890abcdef01234567\"
                      :auth {:type :basic, :user \"user\", :password \"password\"}}
                     {:id :resource3
                      :url \"ftp://example.com/resource3\"
                      :sha256 \"23456789abcdef01234567890abcdef01234567890abcdef01234567890abcde\"
                      :auth {:user \"user\", :password \"password\"}}
                     {:id :resource4
                      :url \"http://example.com/resource4.gz\"
                      :sha1 \"3456789abcdef01234567890abcdef0123456789\"
                      :packed :gzip}]
        :download-to \".cavia\"})"
  [name profile]
  `(let [profile# (merge skeleton-profile ~profile)]
     (def ~name (with-meta profile# {:tag ::Profile}))))

(def ^:private ^:dynamic *tacit-profile* nil)

(defmacro with-profile
  "The specified profile will be used in cavia processes when each profile will
  not be provided.

  e.g.:

      (with-profile prof
        (cavia/clean!)
        (cavia/get!))"
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

;; ## Resource path

(defn- resource*
  [profile id]
  (let [{:keys [resources download-to]} profile]
    (if-let [id* (->> resources
                      (filter #(= (:id %) id))
                      (first)
                      (:id))]
      (.getAbsolutePath (io/file (str download-to "/" (name id*)))))))

(defn- resource-download [profile id]
  (format "%s.download" (resource* profile id)))

(defn- resource-unverified [profile id]
  (format "%s.unverified" (resource* profile id)))

(defmulti resource
  "Returns the local path of the specified resource. Returns nil if the resource
  is not defined in your profile. Note that this function will return the path
  even if the resource has not been downloaded yet."
  meta-tag)

(defmethod resource ::Profile
  [profile id]
  (resource* profile id))

(defmethod resource :default
  [id]
  (resource* *tacit-profile* id))

;; ## Resource info

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

;; ## Existence

(defn- exist?*
  [profile id]
  (let [f (resource profile id)]
    (and (not (nil? f)) (.isFile (io/file f)))))

(defn- exist-unverified?
  [profile id]
  (let [f (resource-unverified profile id)]
    (and (not (nil? f)) (.isFile (io/file f)))))

(defmulti exist?
  "Returns true if the specified resource exists on local."
  meta-tag)

(defmethod exist? ::Profile
  [profile id]
  (exist?* profile id))

(defmethod exist? :default
  [id]
  (exist?* *tacit-profile* id))

;; ## Validation

(def ^:private hash-algo-order
  "Available hash algorithms with priority values. An algorithm with larger
  priority will be used."
  {:md5 0
   :sha1 1
   :sha256 2})

(defn- enabled-hash
  [r]
  (->> (select-keys r [:md5 :sha1 :sha256])
       (sort-by #((first %) hash-algo-order) >)
       first))

(defn- hash-file
  [f algo]
  (binding [digest/*buffer-size* 8192]
    ((case algo
       :md5 digest/md5
       :sha1 digest/sha1
       :sha256 digest/sha-256
       (throw (IllegalArgumentException. (str "Invalid hash algorithm: " algo))))
     (io/file f))))

(defn- print-hash-alert
  ([profile id]
   (let [[ha hv] (->> (:resources profile)
                      (filter #(= (:id %) id))
                      (first)
                      (enabled-hash))
         f (resource* profile id)]
     (print-hash-alert id hv (hash-file f ha))))
  ([id expect-hash actual-hash]
   (binding [*out* *err*]
     (println (str "Invalid hash: " id))
     (println (str "  Expected: " expect-hash))
     (println (str "  Actual: " actual-hash)))))

(defn- print-missing-alert
  [id]
  (binding [*out* *err*]
    (println (str "Missing: " id))))

(defn- valid?*
  [profile id]
  (let [[ha hv] (->> (:resources profile)
                     (filter #(= (:id %) id))
                     (first)
                     (enabled-hash))
        f (resource profile id)]
    (and (exist? profile id) (= (hash-file f ha) hv))))

(defn- valid-unverified?
  [profile id]
  (let [[ha hv] (->> (:resources profile)
                     (filter #(= (:id %) id))
                     (first)
                     (enabled-hash))
        f (resource-unverified profile id)]
    (and (exist-unverified? profile id) (= (hash-file f ha) hv))))

(defmulti valid?
  "Returns true if the resource's real hash is same as the defined hash."
  meta-tag)

(defmethod valid? ::Profile
  [profile id]
  (valid?* profile id))

(defmethod valid? :default
  [id]
  (valid?* *tacit-profile* id))

;; ## Verification

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

;; ## Clean

(defn- clean!*
  ([profile]
   (delete-dir (:download-to profile))
   nil)
  ([profile id]
   (io/delete-file (resource profile id))
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

;; ## Download

(defn- get-resource
  [profile id]
  (let [f    (resource profile id)
        dl-f (resource-download profile id)
        uv-f (resource-unverified profile id)
        {:keys [url auth packed], :as r} (resource-info profile id)]
    (when *verbose*
      (println (format "Retrieving %s from %s" id url)))
    (condp #(%1 %2) (:scheme (uri/uri url))
      #{"http" "https"} (dl/http-download! url dl-f :auth auth :resume true)
      #{"ftp" "ftps"}   (dl/ftp-download! url dl-f :auth auth :resume true)
      #{"sftp"}         (dl/sftp-download! url dl-f auth)
      (throw (java.net.MalformedURLException. "Unsupported protocol")))
    (if packed
      (do (dc/decompress dl-f uv-f packed)
          (io/delete-file dl-f))
      (.renameTo (io/file dl-f) (io/file uv-f)))
    (let [[ha hv] (enabled-hash r)
          act-hash (hash-file uv-f ha)]
      (if (= act-hash hv)
        (.renameTo (io/file uv-f) (io/file f))
        (print-hash-alert id hv act-hash)))))

(defn- get!*
  [profile]
  (let [{:keys [resources download-to]} profile]
    (when-not (.isDirectory (io/file download-to))
      (.mkdir (io/file download-to)))
    (doseq [r resources]
      (let [id (:id r)]
        (cond
          (valid? profile id) (when *verbose*
                                (println (str "Already downloaded: " id)))
          (valid-unverified? profile id) (do
                                           (.renameTo (io/file (resource-unverified profile id))
                                                      (io/file (resource profile id)))
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
