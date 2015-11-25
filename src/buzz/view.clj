(ns buzz.view
  (:import [clojure.lang ExceptionInfo])
  (:require [buzz.core :as c]
            [buzz.html :as h]
            [buzz.tree :as t]
            [clojure.algo.generic.functor :refer [fmap]]
            [hickory.convert]
            [hickory.render]
            [net.cgrand.enlive-html :as enlive]
            [schema.core :as s]
            [clojure.pprint]))

(def ViewPlan
  {:name      s/Keyword
   :children [s/Keyword]
   :query     s/Any
   :data      s/Any
   :model     s/Any
   :html      s/Str})

(def ViewPlans {s/Keyword ViewPlan})

(def steps
  [:walk
   :queries
   :fetches
   :expands
   :renders
   :extracts])

(s/defn pipeline :- h/Html
  [page-tree :- t/Tree
   views     :- {s/Keyword c/View?}]
  (let [view-plans (into
                    (array-map)
                    (t/reverse-level-order-walk
                     (fn [n]
                       (let [v (t/value-of n)
                             children (mapv t/value-of (t/children-of n))]
                         [v {:name     v
                             :view     (get views v)
                             :children children
                             :query    nil
                             :data     nil
                             :model    nil
                             :html     nil}]))
                     page-tree))
        view-plans (fmap
                    (fn [{:keys [view] :as plan}]
                      (assoc plan :queries (c/requires view nil nil)))
                    view-plans)
        view-plans (fmap
                    (fn [{:keys [query] :as plan}]
                      (let [data (c/fetch query)]
                        (assoc plan :data data)))
                    view-plans)
        view-plans (fmap
                    (fn [{:keys [view data] :as plan}]
                      (assoc plan :model (c/expands view data)))
                    view-plans)
        view-plans (reduce
                    (fn [vps [key {:keys [name view model children] :as plan}]]
                      (let [child-views (select-keys vps children)
                            child-htmls (fmap :html child-views)
                            html (c/renders view model child-htmls)]
                        (assoc-in vps [name :html] html)))
                    view-plans
                    view-plans)
        root (first page-tree)]
    (get-in view-plans [root :html])))

(s/defn ->html :- s/Str
  [h :- h/Html]
  (let [content (cond (sequential? h)
                      h
                      (= (:type h) :document)
                      (:content h)
                      (= (:tag h) :html)
                      [h]
                      :else (throw (ExceptionInfo. "cannot serialize" h)))]
    (reduce str (enlive/emit* content))))
