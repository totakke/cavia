(defproject cavia "0.2.2"
  :description "A test resource manager for Clojure"
  :url "https://totakke.github.io/cavia/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [pandect "0.5.4"]
                 [clj-http-lite "0.3.0"]
                 [com.cemerick/url "0.1.1"]
                 [commons-net "3.4"]
                 [progrock "0.1.1"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
