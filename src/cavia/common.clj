(ns cavia.common)

(def ^:dynamic *verbose*
  "A boolean value representing verbosity."
  true)

(def ^:dynamic *download-buffer-size*
  "A buffer size using in a downloading process. Its unit is byte."
  1024)
