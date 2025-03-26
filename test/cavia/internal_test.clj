(ns cavia.internal-test
  (:require [cavia.internal :as i]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]
            [lambdaisland.uri :as uri]))

(stest/instrument)

(deftest parse-auth-test
  (are [u a e] (= (i/parse-auth (uri/uri u) a) e)
    "ftp://example.com"       nil                         nil
    "ftp://u1:p1@example.com" nil                         {:user "u1" :password "p1"}
    "ftp://example.com"       {:user "u2" :password "p2"} {:user "u2" :password "p2"}
    "ftp://u1:p1@example.com" {:user "u2" :password "p2"} {:user "u2" :password "p2"}))
