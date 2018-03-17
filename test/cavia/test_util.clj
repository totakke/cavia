(ns cavia.test-util
  (:require [clojure.java.io :as io])
  (:import [org.mockftpserver.fake FakeFtpServer UserAccount]
           [org.mockftpserver.fake.filesystem FileEntry UnixFakeFileSystem]))

(defmacro with-out-null
  [& body]
  `(binding [*out* (io/writer "/dev/null")
             *err* (io/writer "/dev/null")]
     ~@body))

(def temp-dir (.getPath (io/file (System/getProperty "java.io.tmpdir") "cavia-test")))

(defn prepare-cache! []
  (.mkdir (io/file temp-dir)))

(defn clean-cache! []
  (let [dir (io/file temp-dir)]
    (when (.exists dir)
      (doseq [f (seq (.list dir))]
        (.delete (io/file (str temp-dir "/" f))))
      (.delete dir))))

(defn ftp-server []
  (doto (FakeFtpServer.)
    (.setServerControlPort 2221)
    (.addUserAccount (UserAccount. "user" "password" "/"))
    (.setFileSystem (doto (UnixFakeFileSystem.)
                      (.add (FileEntry. "/test.txt" "Stay hungry; stay foolish."))))))

(defn ftp-server-fixture [f]
  (let [server (ftp-server)]
    (.start server)
    (f)
    (.stop server)))
