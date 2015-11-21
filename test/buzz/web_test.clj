(ns buzz.web-test
  (:require [buzz.web :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.rose-tree :as rose]
            [clojure.data.generators :as datgen]
            [clojure.test.check.clojure-test :refer (defspec)]
            [schema.core :as s]
            [com.gfredericks.test.chuck :as chuck]
            [clojure.algo.generic.functor :refer [fmap]]
            [buzz.generators :as g]))

(defspec spec-resolve
  (chuck/times 10)
  (prop/for-all [url' g/url]
    true))
