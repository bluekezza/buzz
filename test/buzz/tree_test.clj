(ns buzz.tree-test
  (:require [buzz.tree :refer :all]
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
            [buzz.generators :as g]
            [schema.core :as s]
            [buzz.tree :as tree]))

(s/set-fn-validation! true)

(s/def trees :- [tree/Tree]
 [[1 [[2 []]]]
  [1 [[2 [[3 []]]]]]
  [1 [[2 [[3 []] [4 []]]]]]
  [1 [[2 []] [3 []]]]
  [1 [[2 [[12 []] [34 []]]] [3 [[56 []] [31 []]]]]]])

(deftest reverse-level-order-test
  (let [f tree/reverse-level-order]
    (doseq [{:keys [expected input] :as fixture} [{:expected [2 1]     :input (nth trees 0)}
                                                  {:expected [3 2 1]   :input (nth trees 1)}
                                                  {:expected [3 4 2 1] :input (nth trees 2)}
                                                  {:expected [2 3 1]   :input (nth trees 3)}
                                                  {:expected [12 34 56 31 2 3 1] :input (nth trees 4)}]]
      (is (= expected (f input))))))
