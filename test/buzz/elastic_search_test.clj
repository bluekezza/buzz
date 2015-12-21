(ns buzz.elastic-search-test
  (:require [buzz.elastic-search :as es]
            [clojure.test :refer :all]))

;; https://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-multi-search.html
;; https://github.com/clojurewerkz/elastisch/blob/fd6adafd631afbcf0fbd6bc542a15a0510069b13/test/clojurewerkz/elastisch/native_api/multi_test.clj
;; https://github.com/clojurewerkz/elastisch/blob/fd6adafd631afbcf0fbd6bc542a15a0510069b13/src/clojurewerkz/elastisch/native/multi.clj

(deftest as-multi-search-test
  (let [queries [(es/->SingleQuery "articles" "articles" {:match :articles} 1 nil)
                 (es/->SingleQuery "videos" "videos" {:match :videos} 1 nil)
                 (es/->SingleQuery "topics" "topics" {:match :topics} 1 nil)]
        input (es/->MultiQuery queries)
        output-expected [{:index "articles" :type "articles"}
                         {:query {:match :articles} :size 1 :fields nil}
                         {:index "videos" :type "videos"}
                         {:query {:match :videos} :size 1 :fields nil}
                         {:index "topics" :type "topics"}
                         {:query {:match :topics} :size 1 :fields nil}]
        output-actual (es/for-multi-search queries)
        ]
    (is (= output-expected output-actual))))
