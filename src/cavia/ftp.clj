(ns cavia.ftp
  (:require [lambdaisland.uri :as uri]
            [cavia.internal :refer [str->int parse-auth]])
  (:import [org.apache.commons.net.ftp FTP FTPClient FTPSClient FTPReply]))

(defn client
  "Connects an FTP server specified by `url`, returning
  `org.apache.commons.net.ftp.FTPClient` instance.

  Options:

      :auth        Username and password for FTP authentication.
                   e.g. {:user \"user\", :password \"password\"}

      :file-type   The file type to be transferred: `:binary` or `:ascii`.
                   Default is `:binary`.

      :local-mode  The data connection mode: `:active` or `:passive`. Default is
                   `:passive`."
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
      (.setControlKeepAliveTimeout 300)
      (.setControlKeepAliveReplyTimeout 1000)
      (.setSoTimeout 30000)
      (.setDataTimeout 30000))
    (case local-mode
      :active (.enterLocalActiveMode client)
      :passive (.enterLocalPassiveMode client))
    client))
