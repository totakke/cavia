(ns cavia.util
  (:require [clojure.spec.alpha :as s]))

(defmacro with-connection
  [bindings & body]
  (cond
    (zero? (count bindings)) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-connection ~(subvec bindings 2) ~@body)
                                (finally
                                  (when (.isConnected ~(bindings 0))
                                    (.disconnect ~(bindings 0))))))
    :else (throw (IllegalArgumentException. "with-connection only allows Symbols in bindings"))))

(s/fdef with-connection
  :args (s/cat :bindings (s/and vector?
                                #(even? (count %))
                                (s/* (s/cat :sym simple-symbol?
                                            :connection any?)))
               :body (s/* any?)))
