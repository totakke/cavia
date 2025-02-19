(ns cavia.integration-test
  (:require [cavia.core :as cavia :refer [defprofile with-profile]]
            [cavia.test-util :refer [with-out-null]]
            [clojure.test :refer [deftest is use-fixtures]]))

(defprofile test-prof
  {:resources [{:id :test-resource-ftp
                :url "ftp://localhost:2221/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :auth {:user "user" :password "password"}}
               {:id :test-resource-s3
                :url "http://localhost:9000/cavia/test.png"
                :sha1 "07dba3bd9f227f58134d339b1609e0a913abe0de"
                :protocol :s3
                :auth {:access-key-id "minioadmin"
                       :secret-access-key "minioadmin"}}]})

(defn fixture-cavia [f]
  (with-out-null
    (with-profile test-prof
      (cavia/clean!)
      (cavia/get!)
      (f)
      (cavia/clean!))))

(use-fixtures :once fixture-cavia)

(deftest ^:integration ftp-test
  (is (cavia/exist? :test-resource-ftp))
  (is (cavia/valid? :test-resource-ftp)))

(deftest ^:integration s3-test
  (is (cavia/exist? :test-resource-s3))
  (is (cavia/valid? :test-resource-s3)))
