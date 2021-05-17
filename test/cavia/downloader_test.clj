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

(use-fixtures :once fixture ftp-server-fixture)

;;;
;;; defs
;;;

(def http-test-url "https://s3.amazonaws.com/cavia/test.png")
(def http-test-hash "07dba3bd9f227f58134d339b1609e0a913abe0de")
(def http-test-local (str temp-dir "/http-test-resource"))

(def ftp-test-url "ftp://localhost:2221/test.txt")
(def ftp-test-auth {:user "user" :password "password"})
(def ftp-test-hash "6fa556105227e58c750010e9627a89e172966f82")
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
    (let [http-test-fragment (str temp-dir "/http-test-resource-fragment")]
      (io/copy (io/file (io/resource "test.png.download")) (io/file http-test-fragment))
      (is (nil? (dl/http-download! http-test-url http-test-fragment
                                   :resume (.length (io/file http-test-fragment)))))
      (is (= (sha1-file http-test-fragment) http-test-hash)))))

(deftest ftp-download!-test
  (testing "returns nil when finishing successfully "
    (is (nil? (dl/ftp-download! ftp-test-url ftp-test-local :auth ftp-test-auth))))
  (testing "check the resource's hash"
    (is (= (sha1-file ftp-test-local) ftp-test-hash))))
