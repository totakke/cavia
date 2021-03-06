(defproject cavia "0.6.0"
  :description "Test resource manager for Clojure project"
  :url "https://github.com/totakke/cavia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.12.1"]
                 [com.jcraft/jsch "0.1.55"]
                 [commons-net "3.8.0"]
                 [digest "1.4.10"]
                 [lambdaisland/uri "1.4.54"]
                 [org.apache.commons/commons-compress "1.20"]
                 [progrock "0.1.2"]]
  :profiles {:dev {:dependencies [[org.mockftpserver/MockFtpServer "2.8.0"]]}
             :test {:dependencies [[org.slf4j/slf4j-nop "1.7.30"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}}
  :signing {:gpg-key "roimisia@gmail.com"})
