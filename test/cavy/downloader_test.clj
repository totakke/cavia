(ns cavy.downloader-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [pandect.core :refer [sha1-file]]
            [cavy.downloader :as dl]))

;;;
;;; Setup and teardown
;;;

(def temp-dir (.getPath (file (System/getProperty "java.io.tmpdir") "cavy-test")))

(defn- prepare-cache! []
  (.mkdir (file temp-dir)))

(defn- clean-cache! []
  (let [dir (file temp-dir)]
    (when (.exists dir)
      (doseq [f (seq (.list dir))]
        (.delete (file (str temp-dir "/" f))))
      (.delete dir))))

(defn fixture [f]
  (prepare-cache!)
  (f)
  (clean-cache!))

(use-fixtures :once fixture)

;;;
;;; defs
;;;

(def ftp-test-url "ftp://ftp.funet.fi/pub/misc/ChangeLog")
(def ftp-test-auth {:user "anonymous"})
(def ftp-test-hash "784223e89be5c29d7348b3d644c8dffb52f86aa9")
(def ftp-test-local (str temp-dir "/ftp-test-resource"))

;;;
;;; Tests
;;;

(deftest ftp-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/ftp-download! ftp-test-url ftp-test-local :auth ftp-test-auth))))
  (testing "check the resource's hash"
    (is (= (sha1-file ftp-test-local) ftp-test-hash))))
