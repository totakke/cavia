(ns cavy.util
  (:require [clojurewerkz.urly.core :as urly]))

(defn protocol-of
  "Returns the protocol as Keyword from specified URL. (e.g. :http, :https, etc)"
  [url]
  (keyword (urly/protocol-of (urly/url-like url))))
