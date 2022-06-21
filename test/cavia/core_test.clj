(ns cavia.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cavia.test-util :refer :all]
            [cavia.core :as cavia :refer [defprofile with-profile]]))

(defprofile test-prof
  {:resources [{:id :test-resource
                :url "https://s3.amazonaws.com/cavia/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"}
               {:id :test-resource2
                :url "https://s3.amazonaws.com/cavia/test.png"
                :sha1 "unverifiedsha1"}
               {:id :test-resource3
                :url "https://s3.amazonaws.com/cavia/test.png"
                :md5 "0656b409231ee9bd8c5c9272647c69a1"}
               {:id :test-resource4
                :url "https://s3.amazonaws.com/cavia/test.png"
                :sha256 "55902cd4056e2bd57ced9296c826e4df42f07457c96ce72afe8652d0d6dd89b3"}
               {:id :test-resource-gzip
                :url "https://s3.amazonaws.com/cavia/test.png.gz"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :packed :gzip}
               {:id :test-resource-bzip2
                :url "https://s3.amazonaws.com/cavia/test.png.bz2"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :packed :bzip2}]})

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

(deftest resource-test
  (testing "returns the resource's path"
    (is (not (nil? (re-find #".*\.cavia/test-resource$" (cavia/resource :test-resource))))))
  (testing "return path is absolute"
    (is (.isAbsolute (io/file (cavia/resource :test-resource)))))
  (testing "returns nil when the id does not exist"
    (is (nil? (cavia/resource :notexist)))))

(deftest exist?-test
  (testing "returns true if the file is already downloaded"
    (is (cavia/exist? :test-resource)))
  (testing "returns false if the file is not downloaded"
    (is (not (cavia/exist? :test-resource2)))))

(deftest valid?-test
  (testing "returns true if the file's hash is valid"
    (are [k] (cavia/valid? k)
      :test-resource
      :test-resource3
      :test-resource4))
  (testing "returns false if the file's hash is invalid"
    (is (not (cavia/valid? :test-resource2)))))

(deftest packed-test
  (testing "gzip"
    (is (cavia/exist? :test-resource-gzip))
    (is (cavia/valid? :test-resource-gzip)))
  (testing "bzip2"
    (is (cavia/exist? :test-resource-bzip2))
    (is (cavia/valid? :test-resource-bzip2))))
