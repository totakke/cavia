(ns cavia.core-test
  (:require [cavia.core :as cavia :refer [defprofile with-profile]]
            [cavia.test-util :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))

(stest/instrument)

(defprofile test-prof
  {:resources [{:id :test-resource
                :url "http://localhost:8080/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"}
               {:id :test-resource2
                :url "http://localhost:8080/test.png"
                :sha1 "0000000000000000000000000000000000000000"}
               {:id :test-resource-md5
                :url "http://localhost:8080/test.png"
                :md5 "0656b409231ee9bd8c5c9272647c69a1"}
               {:id :test-resource-sha256
                :url "http://localhost:8080/test.png"
                :sha256 "55902cd4056e2bd57ced9296c826e4df42f07457c96ce72afe8652d0d6dd89b3"}
               {:id :test-resource-sha512
                :url "http://localhost:8080/test.png"
                :sha512 "71122b126cf93561275d711d543bd09db82f68637a603fec49e5e0a1312a1e3749580650d8adbd8989affe036a052019d7602c53f2456f117c1960c35b091ccb"}
               {:id :test-resource-gzip
                :url "http://localhost:8080/test.png.gz"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :packed :gzip}
               {:id :test-resource-bzip2
                :url "http://localhost:8080/test.png.bz2"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :packed :bzip2}
               {:id :test-resource-ftp
                :url "ftp://localhost:2221/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :auth {:user "user" :password "password"}}
               {:id :test-resource-s3
                :url "http://localhost:9000/cavia/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :protocol :s3
                :auth {:access-key-id "minioadmin"
                       :secret-access-key "minioadmin"}}]})

;;;
;;; Setup and teardown
;;;

(defn fixture-cavia [f]
  (with-out-null
    (with-profile test-prof
      (cavia/clean!)
      (cavia/get!)
      (f)
      (cavia/clean!))))

(use-fixtures :once fixture-cavia)

;;;
;;; Tests
;;;

(deftest ^:integration resource-test
  (testing "returns the resource's path"
    (is (not (nil? (re-find #".*\.cavia/test-resource$" (cavia/resource :test-resource))))))
  (testing "return path is absolute"
    (is (.isAbsolute (io/file (cavia/resource :test-resource)))))
  (testing "returns nil when the id does not exist"
    (is (nil? (cavia/resource :notexist)))))

(deftest ^:integration exist?-test
  (testing "returns true if the file is already downloaded"
    (are [k] (true? (cavia/exist? k))
      :test-resource
      :test-resource-ftp
      :test-resource-s3))
  (testing "returns false if the file is not downloaded"
    (is (not (cavia/exist? :test-resource2)))))

(deftest ^:integration valid?-test
  (testing "returns true if the file's hash is valid"
    (are [k] (true? (cavia/valid? k))
      :test-resource
      :test-resource-md5
      :test-resource-sha256
      :test-resource-sha512
      :test-resource-ftp
      :test-resource-s3))
  (testing "returns false if the file's hash is invalid"
    (is (not (cavia/valid? :test-resource2)))))

(deftest ^:integration packed-test
  (testing "gzip"
    (is (cavia/exist? :test-resource-gzip))
    (is (cavia/valid? :test-resource-gzip)))
  (testing "bzip2"
    (is (cavia/exist? :test-resource-bzip2))
    (is (cavia/valid? :test-resource-bzip2))))
