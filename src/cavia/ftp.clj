(ns cavia.ftp
  (:require [cavia.internal :refer [str->int parse-auth]]
            [cavia.specs :as specs]
            [clojure.spec.alpha :as s]
            [lambdaisland.uri :as uri])
  (:import java.time.Duration
           [org.apache.commons.net.ftp FTP FTPClient FTPSClient FTPReply]))

(defn client
  ^FTPClient
  [url & [{:keys [auth file-type local-mode]
           :or {file-type :binary, local-mode :passive}}]]
  (let [u (uri/uri url)
        ^FTPClient client (case (:scheme u)
                            "ftp" (FTPClient.)
                            "ftps" (FTPSClient.)
                            (throw (ex-info "Unexpected scheme" {:scheme (:scheme u)})))
        auth (or (parse-auth u auth) {:user "anonymous" :password nil})
        port (int (or (str->int (:port u)) 21))]
    (.connect client ^String (:host u) port)
    (let [reply (.getReplyCode client)]
      (when-not (FTPReply/isPositiveCompletion reply)
        (throw (ex-info "Connection refused" {:reply-code reply
                                              :reply-string (.getReplyString client)}))))
    (doto client
      (.login (:user auth) (:password auth))
      (.setFileType (case file-type
                      :binary FTP/BINARY_FILE_TYPE
                      :ascii FTP/ASCII_FILE_TYPE))
      (.setControlKeepAliveTimeout (Duration/ofMillis 300))
      (.setControlKeepAliveReplyTimeout (Duration/ofSeconds 1))
      (.setSoTimeout 30000)
      (.setDataTimeout (Duration/ofSeconds 30)))
    (case local-mode
      :active (.enterLocalActiveMode client)
      :passive (.enterLocalPassiveMode client))
    client))

(s/fdef client
  :args (s/cat :url ::specs/url
               :opts (s/keys :opt-un [:cavia.specs.ftp/auth
                                      :cavia.specs.ftp/file-type
                                      :cavia.specs.ftp/local-mode]))
  :ret #(instance? FTPClient %))
