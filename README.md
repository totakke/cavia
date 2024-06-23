# Cavia

[![Clojars Project](https://img.shields.io/clojars/v/cavia.svg)](https://clojars.org/cavia)
[![cljdoc badge](https://cljdoc.org/badge/cavia/cavia)](https://cljdoc.org/d/cavia/cavia)
[![build](https://github.com/totakke/cavia/actions/workflows/build.yml/badge.svg)](https://github.com/totakke/cavia/actions/workflows/build.yml)

Remote resource management for Clojure projects.

## Rationale

In some cases, tests of a project require large-size files. Among other things,
codes for parsing and I/O should be tested by various kinds of files. But
generally, SCM is not good for controlling such large test files. One of the
solutions is using other tools like git-annex or Git LFS. Some Clojurians,
however, may think that they want to solve it in the Clojure ecosystem.

Cavia is useful for such developers. Cavia is written in Clojure so that it can
be directly used in a project and source codes. Cavia downloads test resources
from remotes, checks their hash before tests, and provides convenience
functions to access the resources.

## NOTICE

The OSS license of Cavia was changed to the [MIT License](LICENSE) since v0.7.0.

## Installation

Cavia is available as a Maven artifact from [Clojars](http://clojars.org/cavia).

Clojure CLI/deps.edn (as Git):

```clojure
io.github.totakke/cavia {:git/tag "v0.7.1" :git/sha "7866700"}
```

Clojure CLI/deps.edn (as Maven):

```clojure
cavia/cavia {:mvn/version "0.7.1"}
```

Leiningen/Boot:

```clojure
[cavia "0.7.1"]
```

## Basic usage

### Define resources profile

First, load `cavia.core` and prepare resources' information with `defprofile`
macro.

```clojure
(require '[cavia.core :as cavia :refer [defprofile]])

(defprofile prof
  {:resources [;; Simple HTTP
               {:id :resource1
                :url "http://example.com/resource1"
                :sha256 "0123456789abcdef01234567890abcdef01234567890abcdef01234567890abc"}

               ;; Basic authorization
               {:id :resource2
                :url "http://example.com/resource2"
                :sha1 "123456789abcdef01234567890abcdef01234567"
                :auth {:type :basic, :user "user", :password "password"}}

               ;; FTP
               {:id :resource3
                :url "ftp://example.com/resource3"
                :sha256 "23456789abcdef01234567890abcdef01234567890abcdef01234567890abcde"
                :auth {:user "user", :password "password"}}

               ;; S3
               {:id :resource4
                :url "https://bucket-name.s3.region.amazonaws.com/resource4"
                :sha1 "3456789abcdef01234567890abcdef0123456789"
                :protocol :s3
                :auth {:access-key-id "accesskey", :secret-access-key "secretkey"}}

               ;; Compressed resource
               {:id :resource5
                :url "http://example.com/resource5.gz"
                :sha1 "456789abcdef01234567890abcdef0123456789a"
                :packed :gzip}]
   :download-to ".cavia"})
```

Resources are defined in `:resources` as a vector including some maps. Each
resource map must have `:id :url :md5/:sha1/:sha256` fields, which are
mandatory. `:id` should be specified as keyword or string, which is used for
resource access and downloading file name.

MD5, SHA1, and SHA256 are supported as hash algorithms for verifying files. One
algorithm must be specified at least. If more than one algorithm are specified,
a stronger algorithm will be used: MD5 < SHA1 < SHA256.

`:auth` field is optional. It can be used for password authentication.
Cavia is now supporting HTTP/HTTPS/FTP/FTPS/S3 protocols and Basic/Digest/OAuth2
authentications. A resource that `:packed` specified will be uncompressed after
downloading. Only gzip (`:gzip`) format is supported.

Cavia downloads resources to `:download-to` directory. The default location is
`./.cavia`. Thus maybe you should add `/.cavia` to your SCM ignore list.

### Resource management

Cavia provides some functions for managing resources.

```clojure
(cavia/get! prof)   ; downloads missing resources

(cavia/verify prof) ; checks the downloaded resources' hash

(cavia/clean! prof) ; removes the download directory
```

To call Cavia functions without the profile specification, use `with-profile`
macro.

```clojure
(with-profile prof
  (cavia/clean!)
  (cavia/get!))
```

`get!` and other functions output logs and download progress to stdout. To call
the above functions quietly, use `with-verbosity` macro. For example, the
following code suppresses normal messages but displays download progress.

```clojure
(with-verbosity {:message false
                 :progress true}
  (cavia/get! prof))
```

### Resource access

You do not need to remember the downloaded resources' paths any more. `resource`
returns the absolute path to the resource from the specified resource id. It
returns `nil` when the id is not defined.

```clojure
(cavia/resource prof :resource1)
;;=> "/home/totakke/cavia-example/.cavia/resource1"

(cavia/resource prof :undefined)
;;=> nil
```

## Integration with test frameworks

Cavia is a library for management of test resources. It is good to use Cavia
with test frameworks like clojure.test,
[Midje](https://github.com/marick/Midje), etc.

### with clojure.test

```clojure
(ns foo.core-test
  (:require [clojure.test :refer :all]
            [cavia.core :as cavia :refer [defprofile]]))

(defprofile prof
  {:resources [{:id :resource1
                :url "http://example.com/resource1"
                :sha256 "0123456789abcdef01234567890abcdef01234567890abcdef01234567890abc"}]})

(defn fixture-cavia [f]
  (cavia/get! prof)
  (f))

(use-fixtures :once fixture-cavia)

(deftest your-test
  (testing "tests with the cavia's resource"
    (is (= (slurp (cavia/resource prof :resource1)) "resource1's content")))
```

### with Midje

```clojure
(ns foo.t-core
  (:require [midje.sweet :refer :all]
            [cavia.core :as cavia :refer [defprofile with-profile]]))

(defprofile prof
  {:resources [{:id :resource1
                :url "http://example.com/resource1"
                :sha256 "0123456789abcdef01234567890abcdef01234567890abcdef01234567890abc"}]})

(with-profile prof

  (with-state-changes [(before :facts (cavia/get!))]
    (fact "tests for a large file" :slow
      (slurp (cavia/resource :resource1) => "resource1's content")))

  )
```

## Use as a simple downloader

Cavia provides features of file downloading as independent functions.

```clojure
(require '[cavia.downloader as dl])

(dl/http-download! "http://example.com/foobar.txt" "path/to/foobar.txt")
```

HTTP/HTTPS, FTP/FTPS, and S3 downloading functions support a resume option. If
you specify `:resume true`, the functions resume downloading a partially
downloaded file.

```clojure
(dl/s3-download! "https://foo.s3.region.amazonaws.com/bar/foobar.txt"
                 "path/to/foobar.txt"
                 {:access-key-id "accesskey", :secret-access-key "secretkey"}
                 :resume true)
```

## License

Copyright 2014 [Toshiki Takeuchi](https://totakke.net/)

Distributed under the [MIT License](LICENSE).
