(defproject cavia "0.1.4"
  :description "A test resource manager for Clojure"
  :url "https://totakke.github.io/cavia/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/fs "1.4.6"]
                 [pandect "0.5.1"]
                 [clj-http "1.0.1"]
                 [com.velisco/clj-ftp "0.3.3"]
                 [clojurewerkz/urly "1.0.0"]]
  :signing {:gpg-key "roimisia@gmail.com"})
