(ns cavia.decompressor
  (:require [cavia.common :refer :all]
            [cavia.specs :as specs]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s])
  (:import [java.io FileInputStream FileOutputStream]
           org.apache.commons.compress.compressors.CompressorStreamFactory))

(def compressor-map
  {:gzip  CompressorStreamFactory/GZIP
   :bzip2 CompressorStreamFactory/BZIP2})

(defn decompress
  [^String in-f ^String out-f type]
  (if-let [^String compressor (get compressor-map type)]
    (with-open [in (.createCompressorInputStream (CompressorStreamFactory.)
                                                 compressor
                                                 (FileInputStream. in-f))
                out (FileOutputStream. out-f)]
      (io/copy in out :buffer-size *decompress-buffer-size*))
    (throw (IllegalArgumentException. (str "Unsupported compressor type: " type)))))

(s/fdef decompress
  :args (s/cat :in-f string?
               :out-f string?
               :type ::specs/compressor))
