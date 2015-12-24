(ns buzz.elastic-search-test
  (:require [buzz.elastic-search :as es]
            [clojure.test :refer :all]))

;; https://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-multi-search.html
;; https://github.com/clojurewerkz/elastisch/blob/fd6adafd631afbcf0fbd6bc542a15a0510069b13/test/clojurewerkz/elastisch/native_api/multi_test.clj
;; https://github.com/clojurewerkz/elastisch/blob/fd6adafd631afbcf0fbd6bc542a15a0510069b13/src/clojurewerkz/elastisch/native/multi.clj

(deftest as-multi-search-test
  (let [queries [(es/->SingleQuery "articles" "articles" {:match :articles} 0 1 ["articleId"] {"articleId" :asc})
                 (es/->SingleQuery "videos" "videos" {:match :videos} 0 1 nil nil)
                 (es/->SingleQuery "topics" "topics" {:match :topics} 0 1 nil nil)]
        input (es/->MultiQuery queries)
        output-expected [{:index "articles" :type "articles"}
                         {:query {:match :articles} :from 0 :size 1 :fields ["articleId"] :sort {"articleId" :asc}}
                         {:index "videos" :type "videos"}
                         {:query {:match :videos} :from 0 :size 1 :fields nil :sort nil}
                         {:index "topics" :type "topics"}
                         {:query {:match :topics} :from 0 :size 1 :fields nil :sort nil}]
        output-actual (es/for-multi-search queries)]
    (is (= output-expected output-actual))))
