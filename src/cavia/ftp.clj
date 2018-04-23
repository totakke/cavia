(ns cavia.ftp
  (:require [lambdaisland.uri :as uri]
            [cavia.internal :refer [str->int url-decode]])
  (:import [org.apache.commons.net.ftp FTP FTPClient FTPSClient FTPReply]))

(defn- parse-auth
  [uri auth]
  (cond
    auth (-> auth
             (update :user url-decode)
             (update :password url-decode))
    (or (:user uri) (:password uri)) (select-keys uri [:user :password])
    :else {:user "anonymous" :password nil}))

(defn ^FTPClient client
  [url {:keys [auth file-type local-mode]
        :or {file-type :binary, local-mode :passive}}]
  (let [u (uri/uri url)
        ^FTPClient client (case (:scheme u)
                            "ftp" (FTPClient.)
                            "ftps" (FTPSClient.)
                            (throw (ex-info "Unexpected scheme" {:scheme (:scheme u)})))
        auth (parse-auth u auth)
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
      (.setControlKeepAliveTimeout 300)
      (.setControlKeepAliveReplyTimeout 1000)
      (.setSoTimeout 30000)
      (.setDataTimeout 30000))
    (case local-mode
      :active (.enterLocalActiveMode client)
      :passive (.enterLocalPassiveMode client))
    client))

(defmacro with-connection
  [bindings & body]
  (cond
    (zero? (count bindings)) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-connection ~(subvec bindings 2) ~@body)
                                (finally
                                  (when (.isConnected ~(bindings 0))
                                    (.disconnect ~(bindings 0))))))
    :else (throw (IllegalArgumentException. "with-connection only allows Symbols in bindings"))))
