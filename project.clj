(defproject cavia "0.8.0-SNAPSHOT"
  :description "Remote resource management for Clojure projects"
  :url "https://github.com/totakke/cavia"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[clj-http "3.13.0"]
                 [com.cognitect.aws/api "0.8.692"]
                 [com.cognitect.aws/endpoints "871.2.30.11"]
                 [com.cognitect.aws/s3 "871.2.30.11"]
                 [com.github.mwiede/jsch "0.2.24"]
                 [commons-net "3.11.1"]
                 [lambdaisland/uri "1.19.155"]
                 [org.apache.commons/commons-compress "1.27.1"]
                 [org.clj-commons/digest "1.4.100"]
                 [org.clojure/clojure "1.12.0"]
                 [progrock "1.0.0"]]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.3"]]}
             :1.12 {:dependencies [[org.clojure/clojure "1.12.0"]]}}
  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo/"
                                      :username [:env/clojars_username]
                                      :password [:env/clojars_password]}]]
  :signing {:gpg-key "roimisia@gmail.com"})
