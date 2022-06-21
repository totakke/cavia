(ns cavia.sftp
  (:require [lambdaisland.uri :as uri]
            [cavia.internal :refer [str->int parse-auth]])
  (:import [com.jcraft.jsch ChannelSftp JSch Session]))

(defn session
  ^Session
  [url & [{:keys [auth]}]]
  (let [u (uri/uri url)
        auth (parse-auth u auth)
        port (or (str->int (:port u)) 22)]
    (doto (.getSession (JSch.) (:user auth) (:host u) port)
      (.setPassword ^String (:password auth))
      (.setConfig "StrictHostKeyChecking" "no")
      (.connect))))

(defn channel
  ^ChannelSftp
  [^Session session]
  (doto (.openChannel session "sftp")
    (.connect)))
