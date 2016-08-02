(ns cavia.decompressor
  (:import [java.io FileInputStream FileOutputStream]
           [java.util.zip GZIPInputStream]))

(def ^:const buf-size 4096)

(defn decompress-gzip
  [in-f out-f]
  (let [buffer (byte-array buf-size)]
    (with-open [in (GZIPInputStream. (FileInputStream. ^String in-f))
                out (FileOutputStream. ^String out-f)]
      (loop [len (.read in buffer)]
        (when (pos? len)
          (.write out buffer 0 len)
          (recur (.read in buffer)))))))
