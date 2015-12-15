(ns buzz.view
  (:import
   [clojure.lang ExceptionInfo])
  (:require
   [buzz.core :as c]
   [buzz.html :as h]
   [buzz.http :as http]
   [buzz.tree :as t]
   [clojure.algo.generic.functor :refer [fmap]]
   [clojure.pprint]
   [hickory.convert]
   [hickory.render]
   [net.cgrand.enlive-html :as e]
   [schema.core :as s]))

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
  [page-tree  :- t/Tree
   views      :- {s/Keyword c/View?}
   app-config :- c/Map
   request    :- http/Request]
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
                    (fn [{{configs :configs :as view} :view :as plan}]
                      (let [view-config (fmap #(get-in app-config %) configs)]
                        (assoc plan :queries (c/requires view view-config request))))
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

(s/defn html-as-str :- s/Str
  [h :- h/Html]
  (let [content (cond (sequential? h)
                      h
                      (= (:type h) :document)
                      (:content h)
                      (= (:tag h) :html)
                      [h]
                      :else (throw (ExceptionInfo. "cannot serialize" h)))]
    (apply c/str' (e/emit* content))))

(defn transform-by-id
  [html ;- c/Html
   m    ;- {s/Keyword c/Html}
   ]
  (if-not (seq m)
    html
    (reduce (fn [nodes [k v]]
              (let [selector (keyword (str "#" (name k)))]
                (e/transform nodes [selector] (fn [& _] v)))) ;;replaces the node
            html
            m)))
