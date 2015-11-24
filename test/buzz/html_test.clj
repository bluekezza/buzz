(ns buzz.html-test
  (:require [buzz.html :refer :all]
            [clojure.test :refer :all]))

(deftest element-test
  (testing "zero arguments"
    (is (= {:tag :div
            :type :element
            :attrs {}
            :content []
            } (div))))
  (testing "one argument"
    (is (= {:tag :div
            :type :element
            :attrs {}
            :content []
            } (div {}))))
  (testing "two arguments"
    (is (= {:tag :div
            :type :element
            :attrs {}
            :content ["cheese"]
            } (div {} "cheese"))))
  (testing "attributes and one argument"
    (is (= {:tag :div
            :type :element
            :attrs {:class "header"}
            :content ["cheese"]
            } (div {:class "header"} "cheese"))))
  (testing "nested elements"
    (is (= {:tag :div
            :type :element
            :attrs {:class "header"}
            :content [{:tag :span
                       :type :element
                       :attrs {}
                       :content []}
                      {:tag :img
                       :type :element
                       :attrs {}
                       :content []}]
            } (div {:class "header"} (span) (img)))))
  (testing "vector of child elements"
    (is (= {:tag :div
            :type :element
            :attrs {:class "header"}
            :content [{:tag :span
                       :type :element
                       :attrs {}
                       :content []}
                      {:tag :img
                       :type :element
                       :attrs {}
                       :content []}]}
           (div {:class "header"} [(span) (img)]))))
  (testing "nested children with no values"
    (is (= {:tag :div
            :type :element
            :attrs {}
            :content [{:tag :span
                       :type :element
                       :attrs {}
                       :content []
                       }]
            } (div {} (span {}))))))
