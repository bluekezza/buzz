(ns buzz.view-test
  (:require [clojure.test :refer :all]
            [buzz.view :as v]
            [buzz.core :as c]
            [schema.core :as s]
            [buzz.tree :as t]))

(s/set-fn-validation! true)

(defrecord Header []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    [:div {} "header"]))

(defrecord Content []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    [:div {} "content"]))

(defrecord Footer []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
    [:div {} "footer"]))

(defrecord Page []
  c/View
  (requires [this cfg req] nil)
  (expands [this data] nil)
  (renders [this model children]
   [[:head {}]
    [:body {}
     (:header children)
     (:content children)
     (:footer children)
     ]]))

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
  [[:head {}]
   [:body {}
    [:div {} "header"]
    [:div {} "content"]
    [:div {} "footer"]]])

(deftest pipeline-test
  (is (= render-tree (v/pipeline page views))))

(def render-html
  "<!DOCTYPE html>\n<html><head></head><body><div>header</div><div>content</div><div>footer</div></body></html>")

(deftest html-test
  (is (= render-html (v/->html render-tree))))
