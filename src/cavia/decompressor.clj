(ns cavia.decompressor
  (:require [clojure.java.io :as io]
            [cavia.common :refer :all])
  (:import [java.io FileInputStream FileOutputStream]
           org.apache.commons.compress.compressors.CompressorStreamFactory))

(def compressor-map
  {:gzip  CompressorStreamFactory/GZIP
   :bzip2 CompressorStreamFactory/BZIP2})

(defn decompress
  [^String in-f ^String out-f type]
  (if-let [compressor (get compressor-map type)]
    (with-open [in (.createCompressorInputStream (CompressorStreamFactory.)
                                                 compressor
                                                 (FileInputStream. in-f))
                out (FileOutputStream. out-f)]
      (io/copy in out :buffer-size *decompress-buffer-size*))
    (throw (IllegalArgumentException. (str "Unsupported compressor type: " type)))))
