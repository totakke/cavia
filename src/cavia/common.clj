(ns cavia.common)

(def ^:dynamic *verbose*
  "A boolean value representing verbosity."
  true)

(def ^:dynamic *download-buffer-size*
  "A buffer size used in a downloading process. Its unit is byte."
  4096)

(def ^:dynamic *decompress-buffer-size*
  "A buffer size used in a decompression process. Its unit is byte."
  8192)
