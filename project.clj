(defproject cavia "0.3.2-SNAPSHOT"
  :description "A test resource manager for Clojure"
  :url "https://totakke.github.io/cavia/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/fs "1.4.6"]
                 [clj-http-lite "0.3.0"]
                 [com.cemerick/url "0.1.1"]
                 [commons-net "3.6"]
                 [digest "1.4.5"]
                 [progrock "0.1.2"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0-alpha14"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
