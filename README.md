# Cavia

[![Clojars Project](https://img.shields.io/clojars/v/cavia.svg)](https://clojars.org/cavia)
[![cljdoc badge](https://cljdoc.org/badge/cavia/cavia)](https://cljdoc.org/d/cavia/cavia)
[![build](https://github.com/totakke/cavia/actions/workflows/build.yml/badge.svg)](https://github.com/totakke/cavia/actions/workflows/build.yml)

Remote resource management for Clojure projects.

## Rationale

When testing projects, large-size files are sometimes required. Parsing and I/O
code, in particular, benefits from testing with a variety of file types.
However, traditional Source Code Management (SCM) systems are often not ideal
for handling these large test files. Solutions like git-annex or Git LFS are
commonly used.

Some Clojure developers, however, might prefer a solution within the Clojure
ecosystem. Cavia is designed for these developers. Written entirely in Clojure,
Cavia can be seamlessly integrated into projects and source code. It works by
downloading test resources from remote locations, verifying their integrity
using hash checks before testing, and providing convenient functions for
accessing those resources.

## Installation

Clojure CLI/deps.edn (as Git):

```clojure
io.github.totakke/cavia {:git/tag "v0.8.0" :git/sha "4c5e8ef"}
```

Clojure CLI/deps.edn (as Maven):

```clojure
cavia/cavia {:mvn/version "0.8.0"}
```

Leiningen/Boot:

```clojure
[cavia "0.8.0"]
```

## Basic usage

### Resource profile

First, load `cavia.core` and prepare resources information with `defprofile`
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
                :sha512 "456789abcdef01234567890abcdef0123456789abcdef01234567890abcdef01234567890abcdebcdef01234567890abcdef01234567890abcdebcdef0123456"
                :packed :gzip}]
   :download-to ".cavia"})
```

Resources are defined in `:resources` as a vector including some maps. Each
resource map must have `:id`, `:url`, and a hash like `:sha1`.

| key | required? | description |
| --- | --------- | ----------- |
| `:id` | **Yes** | `:id` should be specified as a keyword or a string, which is used for resource access and downloading a file. |
| `:url` | **Yes** | A URL string. Cavia is now supporting HTTP/HTTPS/FTP/FTPS/S3 protocols. |
| hash | **Yes** | MD5 (`:md5`), SHA1 (`:sha1`), SHA256 (`:sha256`), and SHA512 (`:sha512`) are supported as a hash algorithm for verifying files. One algorithm must be specified at least. If more than one algorithm are specified, a stronger algorithm will be used. |
| `:protocol` | Optional | The specified protocol is used for downloading. `:http`, `:ftp`, `:sftp`, and `:s3` are supported. If `:protocol` is not specified, a protocol inferred from `:url` is used. |
| `:auth` | Optional | `:auth` is used for password authentication. Cavia is now supporting Basic/Digest/OAuth2 authentications. |
| `:packed` | Optional | A resource that `:packed` specified will be uncompressed after downloading. gzip (`:gzip`) and bzip2 (`:bzip2`) formats are supported. |

The downloads resources are saved into `:download-to` directory. The default
location is `./.cavia`. You should add `/.cavia` to your SCM ignore list.

### Resource management

Cavia provides some functions for managing resources.

```clojure
(cavia/get! prof)   ; downloads missing resources

(cavia/verify prof) ; checks the downloaded resources hash

(cavia/clean! prof) ; removes the download directory
```

To call Cavia functions without the profile specification, use `with-profile`
macro.

```clojure
(with-profile prof
  (cavia/clean!)
  (cavia/get!))
```

Some functions output logs and download progress to stdout. To call the
functions quietly, use `with-verbosity` macro. For example, the following code
suppresses the normal messages but displays the download progress.

```clojure
(with-verbosity {:message false
                 :download true}
  (cavia/get! prof))
```

### Resource access

You do not need to remember the downloaded resource paths any more. `resource`
returns the absolute path to the resource from the specified resource id. It
returns `nil` when the id is not defined.

```clojure
(cavia/resource prof :resource1)
;;=> "/home/totakke/cavia-example/.cavia/resource1"

(cavia/resource prof :undefined)
;;=> nil
```

## Using with a test framework

### clojure.test

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

### Midje

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

## Using a simple downloader

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

## Development

### Integration test

To run integration tests testing FTP and S3 protocols, launch mock servers with
Docker Compose first.

```sh
docker compose up -d
lein test :integration
```

## License

Copyright 2014 [Toshiki Takeuchi](https://totakke.net/)

Distributed under the [MIT License](LICENSE).
