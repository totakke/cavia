# cavia

[![Clojars Project](https://img.shields.io/clojars/v/cavia.svg)](https://clojars.org/cavia)
[![Build Status](https://travis-ci.org/totakke/cavia.svg?branch=master)](https://travis-ci.org/totakke/cavia)
[![Dependency Status](https://www.versioneye.com/user/projects/54f98ad74f3108b7d2000231/badge.svg)](https://www.versioneye.com/user/projects/54f98ad74f3108b7d2000231)

Test resource manager for Clojure project.

## Rationale

In some cases, tests of a project require large-size files. Codes for parsing,
I/O, etc. should be tested by various kinds of files. But generally, SCM is not
good for controlling such large test files. One of the solutions is using other
tools like git-annex or Git LFS. Some Clojurians, however, may think that they
want to solve it in the Clojure ecosystem. cavia is useful for such developers.
cavia is written by Clojure so that it can be directly used in a project and
source codes. cavia downloads test resources from remotes and checks their hash
before tests and provides convenience functions to access the resources.

## Installation

cavia is available as a Maven artifact from [Clojars](http://clojars.org/cavia).

With Leiningen/Boot:

```clojure
[cavia "0.4.1"]
```

## Usage

### Define resources profile

First, load `cavia.core` and prepare resources' information with `defprofile` macro.

```Clojure
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
               ;; Compressed source
               {:id :resource4
                :url "http://example.com/resource4.gz"
                :sha1 "3456789abcdef01234567890abcdef0123456789"
                :packed :gzip}]
   :download-to ".cavia"})
```

Resources are defined in `:resources` as a vector including some maps. Each
resource map must have `:id :url :md5/:sha1/:sha256` fields. These fields are
mandatory. `:id` should be specified as keyword or string. It is used for
resource access and downloading file name.

MD5, SHA1, and SHA256 are supported as hash algorithms for verifying files. One
algorithm must be specified at least. If more than one algorighm are specified,
a stronger algorithm will be used: MD5 < SHA1 < SHA256.

`:auth` field is optional. It can be used for password authentication.
cavia is now supporting HTTP/HTTPS/FTP/FTPS protocols and Basic/Digest
authentications. A resource that `:packed` specified will be uncompressed after
downloading. Only gzip (`:gzip`) format is supported.

cavia downloads resources to `:download-to` directory. The default location is
`./.cavia`. Thus maybe you should add `/.cavia` to your SCM ignore list.

### Resource management

cavia provides some functions for managing resources.

```Clojure
(cavia/get! prof)   ; downloads missing resources

(cavia/verify prof) ; checks the downloaded resources' hash

(cavia/clean! prof) ; removes the download directory
```

To call cavia functions without the profile specification, use `with-profile`
macro.

```Clojure
(with-profile prof
  (cavia/clean!)
  (cavia/get!))
```

`get!` and other functions output progress and logs' print to stdout. To call
the above functions quietly, use `without-print` macro.

```Clojure
(without-print
  (cavia/get! prof))
```

### Resource access

You do not need to remember the downloaded resources' paths any more. `resource`
returns the absolute path to the resource from the specified resource id. It
returns `nil` when the id is not defined.

```Clojure
(cavia/resource prof :resource1) ; returns "/home/totakke/cavia-example/.cavia/resource1"

(cavia/resource prof :undefined) ; returns nil
```

## Example usage with test frameworks

cavia is a library for management of test resources. It is good to use cavia
with test frameworks like clojure.test,
[Midje](https://github.com/marick/Midje), etc.

### with clojure.test

```Clojure
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

```Clojure
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

## License

Copyright © 2014-2017 Toshiki Takeuchi

Distributed under the Eclipse Public License version 1.0.
