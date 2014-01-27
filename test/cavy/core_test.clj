(ns cavy.core-test
  (:require [clojure.test :refer :all]
            [cavy.test-util :refer :all]
            [cavy.core :as cavy :refer [defprofile with-profile without-print]]))

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

(defn fixture-cavy [f]
  (with-out-null
   (with-profile test-prof
     (cavy/clean!)
     (cavy/get!)
     (f)
     (cavy/clean!))))

(use-fixtures :once fixture-cavy)

;;;
;;; Tests
;;;

(deftest resource-test
  (testing "returns the resource's path"
    (is (not (nil? (re-find #".*\.cavy/test-resource$" (cavy/resource :test-resource))))))
  (testing "returns nil when the id does not exist"
    (is (nil? (cavy/resource :notexist)))))

(deftest exist?-test
  (testing "returns true if the file is already downloaded"
    (is (cavy/exist? :test-resource)))
  (testing "returns false if the file is not downloaded"
    (is (not (cavy/exist? :test-resource2)))))

(deftest valid?-test
  (testing "returns true if the file's hash is valid"
    (is (cavy/valid? :test-resource)))
  (testing "returns false if the file's hash is invalid"
    (is (not (cavy/valid? :test-resource2)))))
