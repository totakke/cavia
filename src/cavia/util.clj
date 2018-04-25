(ns cavia.util)

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
