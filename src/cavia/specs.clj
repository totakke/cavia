(ns cavia.specs
  (:require [clojure.spec.alpha :as s]
            [lambdaisland.uri]))

(s/def ::id (s/or :keyword simple-keyword?
                  :string (s/and string? not-empty)))

(def url-regex
  #"^\A(([^:/?#]+):)?(//([^/?#\\]*))?([^?#]*)?(\?([^#]*))?(#(.*))?\z$")
(s/def ::url-str
  (s/and string? #(re-matches url-regex %)))

(s/def ::uri-inst #(instance? lambdaisland.uri.URI %))

(s/def ::url (s/or :url-str ::url-str
                   :uri-inst ::uri-inst))

(s/def :cavia.specs.auth/type #{:basic :digest :oauth2})
(s/def :cavia.specs.auth/user (s/and string? not-empty))
(s/def :cavia.specs.auth/password (s/and string? not-empty))
(s/def :cavia.specs.auth/token (s/and string? not-empty))
(s/def :cavia.specs.auth/access-key-id (s/and string? not-empty))
(s/def :cavia.specs.auth/secret-access-key (s/and string? not-empty))

(s/def ::auth (s/keys :opt-un [:cavia.specs.auth/type
                               :cavia.specs.auth/user
                               :cavia.specs.auth/password
                               :cavia.specs.auth/token
                               :cavia.specs.auth/access-key-id
                               :cavia.specs.auth/secret-access-key]))

(s/def ::resume (s/or :b boolean?
                      :i integer?))

(s/def ::compressor #{:gzip :bzip2})

(s/def :cavia.specs.profile-resource/packed (s/nilable ::compressor))
(s/def :cavia.specs.profile-resource/protocol
  (s/nilable #{:http :ftp :sftp :s3}))

(s/def :cavia.specs.profile-resource/md5
  (s/and string? #(re-matches #"^[\da-fA-F]{32}$" %)))
(s/def :cavia.specs.profile-resource/sha1
  (s/and string? #(re-matches #"^[\da-fA-F]{40}$" %)))
(s/def :cavia.specs.profile-resource/sha256
  (s/and string? #(re-matches #"^[\da-fA-F]{64}$" %)))
(s/def :cavia.specs.profile-resource/sha512
  (s/and string? #(re-matches #"^[\da-fA-F]{128}$" %)))

(s/def ::profile-resource
  (s/and (s/keys :req-un [::id
                          ::url]
                 :opt-un [::auth
                          :cavia.specs.profile-resource/packed
                          :cavia.specs.profile-resource/protocol
                          :cavia.specs.profile-resource/md5
                          :cavia.specs.profile-resource/sha1
                          :cavia.specs.profile-resource/sha256
                          :cavia.specs.profile-resource/sha512])
         #(some (partial contains? %) #{:md5 :sha1 :sha256 :sha512})))

(s/def :cavia.specs.profile/resources
  (s/and (s/* ::profile-resource)
         #(apply distinct? (map :id %))))

(s/def :cavia.specs.profile/download-to (s/and string? not-empty))

(s/def ::profile (s/keys :opt-un [:cavia.specs.profile/resources
                                  :cavia.specs.profile/download-to]))

(s/def :cavia.specs.downloader/auth (s/nilable ::auth))
(s/def :cavia.specs.downloader/resume (s/nilable ::resume))

(s/def :cavia.specs.ftp/auth (s/nilable ::auth))
(s/def :cavia.specs.ftp/file-type #{:binary :ascii})
(s/def :cavia.specs.ftp/local-mode #{:active :passive})

(s/def :cavia.specs.sftp/auth (s/nilable ::auth))
