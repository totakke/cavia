# cavy

cavy is a manager library for test resources.

## Usage

First, load `cavy.core` and prepare resources' information with `defcavy` macro.

```Clojure
(require '[cavy.core :as cavy :refer [defcavy]])

(defcavy mycavy
  {:resources [{:id "resource1"
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}
               {:id "resource2"
                :url "http://example.com/resource2"
                :sha1 "234567890abcdefghijklmnopqrstuvwxyz12345"}]
   :download-to ".cavy"})
```

The last `defcavy` will be used on all cavy functions.

cavy provides some functions for manage resources.

```Clojure
(cavy/get)    ; downloads missing resources

(cavy/verify) ; check the downloaded resources' hash

(cavy/clean)  ; remove the download directory
```

You do not need to remember the downloaded resources' paths any more.
`cavy.core/resource` returns the path to the resource from the specified resource id.
It returns `nil` when the id is not defined.

```Clojure
(cavy/resource "resource1") ; returns ".cavy/resource1"

(cavy/resource "undefined") ; return nil
```

## With clojure.test

```Clojure
(ns foo.core-test
  (:require [clojure.test :refer :all]
            [cavy.core :as cavy :refer [defcavy]]))

(defcavy mycavy
  {:resources [{:id "resource1"
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(defn fixture-cavy [f]
  (cavy/get)
  (f))

(use-fixtures :once fixture-cavy)

(deftest your-test
  (testing "tests with the cavy's resource"
    (is (= (slurp (cavy/resource "resource1")) "resource1's content")))
```

## With Midje

```Clojure
(ns foo.t-core
  (:require [midje.sweet :refer :all]
            [cavy.core :as cavy :refer [defcavy]]))

(defcavy mycavy
  {:resources [{:id "resource1"
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(with-state-changes [(before :facts (cavy/get))]
  (fact "tests for a large file" :slow
    (slurp (cavy/resource "resource1") => "resource1's content")))
```

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
