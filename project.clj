(defproject cavia "0.6.0-SNAPSHOT"
  :description "Test resource manager for Clojure project"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.8.0"]
                 [com.jcraft/jsch "0.1.54"]
                 [commons-net "3.6"]
                 [digest "1.4.6"]
                 [lambdaisland/uri "1.1.0"]
                 [org.apache.commons/commons-compress "1.16.1"]
                 [progrock "0.1.2"]]
  :profiles {:dev {:dependencies [[org.mockftpserver/MockFtpServer "2.7.1"]]}
             :test {:dependencies [[org.slf4j/slf4j-nop "1.7.25"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
