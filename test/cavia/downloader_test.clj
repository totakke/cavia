(ns cavia.downloader-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
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

(def http-test-url "https://s3.amazonaws.com/cavia/test.png")
(def http-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def http-test-local (str temp-dir "/http-test-resource"))

(def ftp-test-url "ftp://localhost:2221/test.png")
(def ftp-test-auth {:user "user" :password "password"})
(def ftp-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def ftp-test-local (str temp-dir "/ftp-test-resource"))

(defn- sha1-file
  [f]
  (digest/sha1 (io/file f)))

;;;
;;; Tests
;;;

(deftest http-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/http-download! http-test-url http-test-local))))
  (testing "check the resource's hash"
    (is (= (sha1-file http-test-local) http-test-hash)))
  (testing "resume"
    (let [test-fragment (str temp-dir "/http-test-resource-fragment")]
      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/http-download! http-test-url test-fragment
                                   :resume (.length (io/file test-fragment)))))
      (is (= (sha1-file test-fragment) http-test-hash))

      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/http-download! http-test-url test-fragment
                                   :resume true)))
      (is (= (sha1-file test-fragment) http-test-hash)))))

(deftest ^:integration ftp-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/ftp-download! ftp-test-url ftp-test-local :auth ftp-test-auth))))
  (testing "check the resource's hash"
    (is (= (sha1-file ftp-test-local) ftp-test-hash)))
  (testing "resume"
    (let [test-fragment (str temp-dir "/ftp-test-resource-fragment")]
      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/ftp-download! ftp-test-url test-fragment
                                  :auth ftp-test-auth
                                  :resume (.length (io/file test-fragment)))))
      (is (= (sha1-file test-fragment) ftp-test-hash))

      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/ftp-download! ftp-test-url test-fragment
                                  :auth ftp-test-auth
                                  :resume true)))
      (is (= (sha1-file test-fragment) ftp-test-hash)))))
