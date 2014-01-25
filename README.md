# cavy

cavy is a manager library for test resources.

Sometimes ...TODO

## Installation

cavy is available asa a Maven artifact from [Clojars][clojars].

To use with Leiningen, add the following dependency.

```Clojure
[cavy "0.1.0"]
```

## Usage

### Define resources profile

First, load `cavy.core` and prepare resources' information with `defcavy` macro.

```Clojure
(require '[cavy.core :as cavy :refer [defcavy]])

(defcavy mycavy
  {:resources [;; Simple HTTP
               {:id :resource1
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}
               ;; Basic authorization
               {:id :resource2
                :url "http://example.com/resource2"
                :sha1 "234567890abcdefghijklmnopqrstuvwxyz12345"
                :auth {:type :basic, :user "user", :password "password"}}
               ;; FTP
               {:id :resource3
                :url "ftp://example.com/resource3"
                :sha1 "34567890abcdefghijklmnopqrstuvwxyz123456"
                :auth {:user "user", :password "password"}}]
   :download-to ".cavy"})
```

The last `defcavy` will be used on all cavy functions.

Resources are defined in `:resources`.
Each resource must have `:id :url :sha1` fields. These fields are mandatory.
...TODO

cavy now supports HTTP/HTTPS/FTP protocols and Basic/Digest authentications.

### Resource management

cavy provides some functions for manage resources.

```Clojure
(cavy/get!)   ; downloads missing resources

(cavy/verify) ; checks the downloaded resources' hash

(cavy/clean!) ; removes the download directory
```

To call above functions quietly, use `without-print` macro.

```Clojure
(require '[cavy.core :as cavy :refer [defcavy without-print]])
(without-print (cavy/get!))
```

### Resource access

You do not need to remember the downloaded resources' paths any more.
`cavy.core/resource` returns the absolute path to the resource from the specified resource id.
It returns `nil` when the id is not defined.

```Clojure
(cavy/resource :resource1) ; returns "/home/totakke/cavy-example/.cavy/resource1"

(cavy/resource :undefined) ; returns nil
```

## Example usage with test frameworks

cavy is a library for management of test resources.
It is good to use cavy with test frameworks like clojure.test, [Midje][midje], etc.

### with clojure.test

```Clojure
(ns foo.core-test
  (:require [clojure.test :refer :all]
            [cavy.core :as cavy :refer [defcavy]]))

(defcavy mycavy
  {:resources [{:id "resource1"
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(defn fixture-cavy [f]
  (cavy/get!)
  (f))

(use-fixtures :once fixture-cavy)

(deftest your-test
  (testing "tests with the cavy's resource"
    (is (= (slurp (cavy/resource "resource1")) "resource1's content")))
```

### with Midje

```Clojure
(ns foo.t-core
  (:require [midje.sweet :refer :all]
            [cavy.core :as cavy :refer [defcavy]]))

(defcavy mycavy
  {:resources [{:id "resource1"
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(with-state-changes [(before :facts (cavy/get!))]
  (fact "tests for a large file" :slow
    (slurp (cavy/resource "resource1") => "resource1's content")))
```

## License

Copyright © 2014 Toshiki TAKEUCHI

Distributed under the Eclipse Public License either version 1.0 or any later version.

[clojars]: https://clojars.org/FIXME
[midje]: https://github.com/marick/Midje
