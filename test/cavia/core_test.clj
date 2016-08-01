(ns cavia.core-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [cavia.test-util :refer :all]
            [cavia.core :as cavia :refer [defprofile with-profile without-print]]))

(defprofile test-prof
  {:resources [{:id :test-resource
                :url "http://clojure.org/images/clojure-logo-120b.png"
                :sha1 "42b1e4f531273ea38caaa8c0b1f8da554aa0739b"}
               {:id :test-resource2
                :url "http://clojure.org/images/clojure-logo-120b.png"
                :sha1 "unverifiedsha1"}
               {:id :test-resource-gzip
                :url "https://www.gnu.org/software/emacs/manual/ps/elisp.ps.gz"
                :sha1 "15aed8831dd42a196288df838793f30b2587fa61"
                :pack :gzip}]})

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
    (is (fs/absolute? (cavia/resource :test-resource))))
  (testing "returns nil when the id does not exist"
    (is (nil? (cavia/resource :notexist)))))

(deftest exist?-test
  (testing "returns true if the file is already downloaded"
    (is (cavia/exist? :test-resource)))
  (testing "returns false if the file is not downloaded"
    (is (not (cavia/exist? :test-resource2)))))

(deftest valid?-test
  (testing "returns true if the file's hash is valid"
    (is (cavia/valid? :test-resource)))
  (testing "returns false if the file's hash is invalid"
    (is (not (cavia/valid? :test-resource2)))))

(deftest gzip-test
  (testing ""
    (is (cavia/exist? :test-resource-gzip)))
  (testing ""
    (is (cavia/valid? :test-resource-gzip))))
