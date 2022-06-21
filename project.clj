(defproject cavia "0.6.2"
  :description "Test resource manager for Clojure project"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [com.cognitect.aws/api "0.8.561"]
                 [com.cognitect.aws/endpoints "1.1.12.230"]
                 [com.cognitect.aws/s3 "822.2.1145.0"]
                 [com.jcraft/jsch "0.1.55"]
                 [commons-net "3.8.0"]
                 [digest "1.4.10"]
                 [lambdaisland/uri "1.13.95"]
                 [org.apache.commons/commons-compress "1.21"]
                 [progrock "0.1.2"]]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
