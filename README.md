# cavy

cavy is a manager library for test resources.

## Installation

cavy is available as a Maven artifact from [Clojars][clojars].

To use with Leiningen, add the following dependency.

```Clojure
[cavy "0.1.1"]
```

## Usage

### Define resources profile

First, load `cavy.core` and prepare resources' information with `defprofile` macro.

```Clojure
(require '[cavy.core :as cavy :refer [defprofile]])

(defprofile prof
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

Resources are defined in `:resources`.
Each resource must have `:id :url :sha1` fields. These fields are mandatory.
`:id` should be specified as keyword or string. It is used for resource access
and downloading file name.
`:auth` field is optional. It can be used for password authentication.

cavy is now supporting HTTP/HTTPS/FTP protocols and Basic/Digest authentications.

### Resource management

cavy provides some functions for manage resources.

```Clojure
(cavy/get! prof)   ; downloads missing resources

(cavy/verify prof) ; checks the downloaded resources' hash

(cavy/clean! prof) ; removes the download directory
```

To call cavy functions without the profile specification, use `with-profile` macro.

```Clojure
(with-profile prof
  (cavy/clean!)
  (cavy/get!))
```

To call above functions quietly, use `without-print` macro.

```Clojure
(without-print (cavy/get!))
```

### Resource access

You do not need to remember the downloaded resources' paths any more.
`resource` returns the absolute path to the resource from the specified resource id.
It returns `nil` when the id is not defined.

```Clojure
(cavy/resource prof :resource1) ; returns "/home/totakke/cavy-example/.cavy/resource1"

(cavy/resource prof :undefined) ; returns nil
```

## Example usage with test frameworks

cavy is a library for management of test resources.
It is good to use cavy with test frameworks like clojure.test, [Midje][midje], etc.

### with clojure.test

```Clojure
(ns foo.core-test
  (:require [clojure.test :refer :all]
            [cavy.core :as cavy :refer [defprofile with-profile]]))

(defprofile prof
  {:resources [{:id :resource1
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(defn fixture-cavy [f]
  (with-profile prof
    (cavy/get!)
    (f)))

(use-fixtures :once fixture-cavy)

(deftest your-test
  (testing "tests with the cavy's resource"
    (is (= (slurp (cavy/resource :resource1)) "resource1's content")))
```

### with Midje

```Clojure
(ns foo.t-core
  (:require [midje.sweet :refer :all]
            [cavy.core :as cavy :refer [defprofile with-profile]]))

(defprofile prof
  {:resources [{:id :resource1
                :url "http://example.com/resource1"
                :sha1 "1234567890abcdefghijklmnopqrstuvwxyz1234"}]})

(with-profile prof

  (with-state-changes [(before :facts (cavy/get!))]
    (fact "tests for a large file" :slow
      (slurp (cavy/resource :resource1) => "resource1's content")))

  )
```

## License

Copyright © 2014 Toshiki TAKEUCHI

Distributed under the Eclipse Public License version 1.0.

## Special thanks

cavy was developed for tests of [Chrovis][chrovis].
Chrovis is a cloud service of genome analysis and visualization for researchers.
Chrovis is directed by [Xcoo, Inc.][xcoo].

* Xcoo: http://www.xcoo.jp/
* Chrovis: https://chrov.is/

[clojars]: https://clojars.org/cavy
[midje]: https://github.com/marick/Midje
[xcoo]: http://www.xcoo.jp/
[chrovis]: https://chrov.is/
