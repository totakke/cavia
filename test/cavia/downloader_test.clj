(ns cavia.downloader-test
  (:require [cavia.downloader :as dl]
            [cavia.test-util :refer :all]
            [clj-commons.digest :as digest]
            [clojure.java.io :as io]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all])
  (:import java.net.MalformedURLException))

(stest/instrument)

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

(def http-test-url "http://localhost:8080/test.png")
(def http-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def http-test-local (str temp-dir "/http-test-resource"))

(def ftp-test-url "ftp://localhost:2221/test.png")
(def ftp-test-auth {:user "user" :password "password"})
(def ftp-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def ftp-test-local (str temp-dir "/ftp-test-resource"))

(def s3-test-url "http://localhost:9000/cavia/test.png")
(def s3-test-auth {:access-key-id "minioadmin" :secret-access-key "minioadmin"})
(def s3-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def s3-test-local (str temp-dir "/s3-test-resource"))

(defn- sha1-file
  [f]
  (digest/sha1 (io/file f)))

;;;
;;; Tests
;;;

(deftest ^:integration http-download!-test
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

(deftest s3-bucket-key-test
  (are [url] (= (#'dl/s3-bucket-key url) {:bucket "foo", :key "bar/foobar.png"})
    ;; virtual hosted style
    "https://foo.s3.us-east-1.amazonaws.com/bar/foobar.png"
    "https://foo.s3-us-east-1.amazonaws.com/bar/foobar.png"
    "https://foo.s3.amazonaws.com/bar/foobar.png"
    ;; path style
    "https://s3.us-east-1.amazonaws.com/foo/bar/foobar.png"
    "https://s3.amazonaws.com/foo/bar/foobar.png"
    ;; others
    "http://localhost:9000/foo/bar/foobar.png")
  (are [url] (thrown? MalformedURLException (#'dl/s3-bucket-key url))
    "https://s3.us-east-1.amazonaws.com/foobar.png"
    "https://s3.us-east-1.amazonaws.com/foo"
    "http://localhost:9000/foobar.png"
    "http://localhost:9000/foo"))

(deftest ^:integration s3-download!-test
  (testing "returns nil when finishing successfully"
    (is (nil? (dl/s3-download! s3-test-url s3-test-local s3-test-auth))))
  (testing "check the resource's hash"
    (is (= (sha1-file s3-test-local) s3-test-hash)))
  (testing "resume"
    (let [test-fragment (str temp-dir "/s3-test-resource-fragment")]
      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/s3-download! s3-test-url test-fragment s3-test-auth
                                 :resume (.length (io/file test-fragment)))))
      (is (= (sha1-file test-fragment) s3-test-hash))

      (io/copy (io/file (io/resource "test.png.download")) (io/file test-fragment))
      (is (nil? (dl/s3-download! s3-test-url test-fragment s3-test-auth
                                 :resume true)))
      (is (= (sha1-file test-fragment) s3-test-hash)))))
