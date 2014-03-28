(defproject cavia "0.1.2"
  :description "A test resource manager for Clojure"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/fs "1.4.5"]
                 [pandect "0.3.1"]
                 [clj-http "0.9.1"]
                 [com.velisco/clj-ftp "0.3.0"]
                 [clojurewerkz/urly "1.0.0"]]
  :signing {:gpg-key "roimisia@gmail.com"})
