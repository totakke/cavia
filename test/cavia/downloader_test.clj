(ns cavia.downloader-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [digest]
            [cavia.test-util :refer :all]
            [cavia.downloader :as dl]))

;;;
;;; Setup and teardown
;;;

(defn fixture [f]
  (with-out-null
    (prepare-cache!)
    (f)
    (clean-cache!)))

(use-fixtures :once fixture)

;;;
;;; defs
;;;

(def http-test-url "http://clojure.org/images/clojure-logo-120b.png")
(def http-test-hash "42b1e4f531273ea38caaa8c0b1f8da554aa0739b")
(def http-test-local (str temp-dir "/http-test-resource"))

(def ftp-test-url "ftp://ftp.funet.fi/pub/misc/ChangeLog")
(def ftp-test-auth {:user "anonymous"})
(def ftp-test-hash "784223e89be5c29d7348b3d644c8dffb52f86aa9")
(def ftp-test-local (str temp-dir "/ftp-test-resource"))

(defn- sha1-file
  [f]
  (digest/sha1 (file f)))

;;;
;;; Tests
;;;

(deftest http-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/http-download! http-test-url http-test-local))))
  (testing "check the resource's hash"
    (is (= (sha1-file http-test-local) http-test-hash))))

(deftest ftp-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/ftp-download! ftp-test-url ftp-test-local :auth ftp-test-auth))))
  (testing "check the resource's hash"
    (is (= (sha1-file ftp-test-local) ftp-test-hash))))
