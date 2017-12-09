(defproject cavia "0.4.4-SNAPSHOT"
  :description "Test resource manager for Clojure project"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "3.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [commons-net "3.6"]
                 [digest "1.4.6"]
                 [progrock "0.1.2"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
