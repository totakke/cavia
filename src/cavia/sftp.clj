(ns cavia.sftp
  (:require [cavia.internal :refer [str->int parse-auth]]
            [cavia.specs :as specs]
            [clojure.spec.alpha :as s]
            [lambdaisland.uri :as uri])
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

(s/fdef session
  :args (s/cat :url ::specs/url
               :opts (s/keys :opt-un [:cavia.specs.sftp/auth]))
  :ret #(instance? Session %))

(defn channel
  ^ChannelSftp
  [^Session session]
  (doto (.openChannel session "sftp")
    (.connect)))

(s/fdef channel
  :args (s/cat :session #(instance? Session %))
  :ret #(instance? ChannelSftp %))
