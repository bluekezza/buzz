(ns buzz.core-test
  (:require [buzz.core :as c]
            [buzz.elastic-search :as es]
            [clojure.test :refer :all]
            [schema.core :as s]))

(deftest nest-map-test
  (let [input {:name :header
               :map {:articles :QA :topics :QA}}
        output-expected {[:header :articles] :QA, [:header :topics] :QA}
        output-actual (c/nest-map (:name input) (:map input))]
    (is (= output-expected output-actual))))

(deftest flatten-map-test
  (let [input {:header {:articles :Q1, :videos :Q2}
               :footer {:topics :Q3}} ;; #2
        output-expected {[:header :articles] :Q1
                         [:header :videos]   :Q2
                         [:footer :topics]   :Q3} ;; #3
        output-actual (c/flatten-map input)]
    (is (= output-expected output-actual))))

(defrecord MockView
  [inputs requires queries]
  c/View
  (expands [this data] nil)
  (renders [this model children] nil))

(defn mkMockView
  [queries]
  (->MockView
    []
    #(:queries %)
    queries))

(deftest Query-test
  (let [articles [{:id 1, :type :article} {:id 2, :type :article}]
        query (es/->SingleQuery "index" "type" {:match :article} 0 1 nil nil)
        queries {:articles query
                 :foo :bar}
        header-view (mkMockView queries)]
    (is (= nil (s/check c/View? header-view)))))
