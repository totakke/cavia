(ns cavia.core-test
  (:require [clojure.test :refer :all]
            [cavia.test-util :refer :all]
            [cavia.core :as cavia :refer [defprofile with-profile without-print]]))

(defprofile test-prof
  {:resources [{:id :test-resource
                :url "http://clojure.org/space/showimage/clojure-icon.gif"
                :sha1 "f21616d75dc27dd2b89fcdef04177976a5d404c4"}
               {:id :test-resource2
                :url "http://clojure.org/space/showimage/clojure-icon.gif"
                :sha1 "unverifiedsha1"}]})

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
