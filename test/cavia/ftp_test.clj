(ns cavia.ftp-test
  (:require [clojure.test :refer :all]
            [lambdaisland.uri :as uri]
            [cavia.ftp :as ftp]))

(deftest parse-auth-test
  (are [u a e] (= (#'ftp/parse-auth (uri/uri u) a) e)
    "ftp://example.com"       nil                         {:user "anonymous" :password nil}
    "ftp://u1:p1@example.com" nil                         {:user "u1" :password "p1"}
    "ftp://example.com"       {:user "u2" :password "p2"} {:user "u2" :password "p2"}
    "ftp://u1:p1@example.com" {:user "u2" :password "p2"} {:user "u2" :password "p2"}))
