(ns cavia.common)

(def ^:dynamic *verbosity*
  {:message true
   :download true})

(def ^:dynamic *download-buffer-size*
  "A buffer size used in a downloading process. Its unit is byte."
  4096)

(def ^:dynamic *decompress-buffer-size*
  "A buffer size used in a decompression process. Its unit is byte."
  8192)
