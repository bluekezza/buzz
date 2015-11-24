(ns buzz.view-test
  (:require [buzz.core :as c]
            [buzz.html :refer :all]
            [buzz.tree :as t]
            [buzz.view :as v]
            [clojure.test :refer :all]
            [schema.core :as s]))

(s/set-fn-validation! true)

(deftest as-hickory-test
  (is (= [{:type :element, :attrs {:class "footer"}, :tag :div, :content ["footer"]}]
         (as-hickory [[:div {:class "footer"} "footer"]]))))

(deftest as-hickory-test-2
  (is (= {:type :element, :attrs {:class "footer"}, :tag :div, :content ["footer"]}
         (as-hickory [:div {:class "footer"} "footer"]))))

(defrecord Header []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    (div {:class "header"} "header")))

(defrecord Content []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    (div {:class "content"} "content")))

(defrecord Footer []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    (as-hickory
      [:div {:class "footer"} "footer"])))

(defrecord Page []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    (document
     (html {}
      (head {})
      (body {}
        (div {:class "adbanner"})
        (div {:class "page"}
          (vals children)))))))

(s/def views :- {s/Keyword c/View?}
  {:header  (->Header)
   :content (->Content)
   :footer  (->Footer)
   :page    (->Page)})

(s/def page :- t/Tree
  (let [h [:header []]
        c [:content []]
        f [:footer []]
        p [:page [h c f]]]
    p))

(def render-tree
  {:type :document
   :content
   [{:tag :html
     :type :element
     :attrs {}
     :content
     [{:tag :head
       :type :element
       :attrs {}
       :content []}
      {:tag :body
       :type :element
       :attrs {}
       :content
       [{:tag :div
         :type :element
         :attrs {:class "adbanner"}
         :content []}
        {:tag :div
         :type :element
         :attrs {:class "page"}
         :content
         [{:tag :div, :type :element, :attrs {:class "header"}, :content ["header"]}
          {:tag :div, :type :element, :attrs {:class "content"}, :content ["content"]}
          {:tag :div, :type :element, :attrs {:class "footer"}, :content ["footer"]}]}]}]}]})

(deftest pipeline-test
  (is (= render-tree (v/pipeline page views))))

(def render-html
  "<html><head></head><body><div class=\"adbanner\"></div><div class=\"page\"><div class=\"header\">header</div><div class=\"content\">content</div><div class=\"footer\">footer</div></div></body></html>")

(deftest html-test
  (is (= render-html (v/->html (v/pipeline page views)))))
