(ns cavy.core-test
  (:require [clojure.test :refer :all]
            [cavy.core :as cavy :refer [defcavy]]))

(defcavy mycavy
  {:resources [{:id "test-resource"
                :url "http://clojure.org/space/showimage/clojure-icon.gif"
                :sha1 "f21616d75dc27dd2b89fcdef04177976a5d404c4"}
               {:id "test-resource2"
                :url "http://clojure.org/space/showimage/clojure-icon.gif"
                :sha1 "unverifiedsha1"}]})

(defn fixture-cavy [f]
  (cavy/clean)
  (cavy/get)
  (f)
  (cavy/clean))

(use-fixtures :once fixture-cavy)

(deftest resource-test
  (testing "returns the resource's path"
    (is (not (nil? (re-find #".*\.cavy/test-resource$" (cavy/resource "test-resource"))))))
  (testing "returns nil when the id does not exist"
    (is (nil? (cavy/resource "notexist")))))

(deftest exist?-test
  (testing "returns true if the file is already downloaded"
    (is (cavy/exist? "test-resource")))
  (testing "returns false if the file is not downloaded"
    (is (not (cavy/exist? "test-resource2")))))
