(ns buzz.view
  (:require
   [buzz.core :as c]
   [buzz.tree :as t]
   [clojure.algo.generic.functor :refer [fmap]]
   [schema.core :as s]))

(def Hiccup
  "placeholder for a hiccup schema"
  s/Any)

(def ViewPlan
  {:name      s/Keyword
   :children [s/Keyword]
   :query     s/Any
   :data      s/Any
   :model     s/Any
   :html      s/Str})

(def ViewPlans {s/Keyword ViewPlan})

(s/defn pipeline :- Hiccup
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
                    view-plans)]
    (get-in view-plans [:page :html])))

(s/defn ->html :- s/Str
  [h :- Hiccup]
  (c/apply-macro hiccup.page/html5 h))
