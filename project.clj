(defproject cavia "0.1.2-SNAPSHOT"
  :description "A test resource manager for Clojure"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/fs "1.4.5"]
                 [pandect "0.3.0"]
                 [clj-http "0.7.8"]
                 [com.velisco/clj-ftp "0.3.0"]
                 [clojurewerkz/urly "1.0.0"]]
  :signing {:gpg-key "roimisia@gmail.com"})
