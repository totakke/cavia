(defproject cavia "0.7.0"
  :description "Remote resource management for Clojure projects"
  :url "https://github.com/totakke/cavia"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [com.cognitect.aws/api "0.8.692"]
                 [com.cognitect.aws/endpoints "1.1.12.626"]
                 [com.cognitect.aws/s3 "848.2.1413.0"]
                 [com.jcraft/jsch "0.1.55"]
                 [commons-net "3.10.0"]
                 [digest "1.4.10"]
                 [lambdaisland/uri "1.19.155"]
                 [org.apache.commons/commons-compress "1.26.2"]
                 [progrock "0.1.2"]]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}}
  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo/"
                                      :username [:env/clojars_username]
                                      :password [:env/clojars_password]}]]
  :signing {:gpg-key "roimisia@gmail.com"})
