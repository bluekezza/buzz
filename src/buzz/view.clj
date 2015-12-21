(ns buzz.view
  (:import
   [clojure.lang ExceptionInfo])
  (:require
   [buzz.core :as c]
   [buzz.elastic-search :as es]
   [buzz.html :as h]
   [buzz.http :as http]
   [buzz.tree :as t]
   [clojure.algo.generic.functor :refer [fmap]]
   [clojure.pprint :as cpp]
   [hickory.convert]
   [hickory.render]
   [net.cgrand.enlive-html :as e]
   [schema.core :as s]))

(s/defschema Queries {s/Keyword (s/either c/Query? Object)})

(s/defschema ViewName (-> s/Keyword
                          (s/named "ViewName")))

(s/defschema ViewPlan
  {:name      ViewName
   :view      c/View?
   :children [s/Keyword]
   :queries   (s/maybe Queries)
   :data      (s/maybe {s/Keyword s/Any})
   :model     (s/maybe s/Any)
   :html      (s/maybe h/Html)})

(s/defschema ViewPlans
  (-> (s/both clojure.lang.PersistentArrayMap
              {ViewName ViewPlan})
      (s/named "ViewPlans")))

(def ^:private steps
  [:generate-view-plan-from-page-tree
   :required-inputs-to-queries
   :fetch-queries-to-data
   :expands-data-to-model
   :renders-model-to-html])

(s/defn ^:private page-tree->view-plans :- ViewPlans
  [page-tree :- t/Tree
   views      :- {s/Keyword c/View?}]
  (into
   (array-map)
   (t/reverse-level-order-walk
    (fn [n]
      (let [v (t/value-of n)
            children (mapv t/value-of (t/children-of n))]
        [v {:name     v
            :view     (get views v)
            :children children
            :queries  nil
            :data     nil
            :model    nil
            :html     nil}]))
    page-tree)))

(def ^:private AppConfig c/Map)

(defn- trace
  [& vs]
  (binding [*out* *err*]
    (apply cpp/pprint vs)))

(s/defn ^:private require-queries :- ViewPlan
  [config :- AppConfig
   request :- http/Request
   {{inputs :inputs :as view} :view :as plan} :- ViewPlan]
  (let [assets {:request request
                :config config}
        view-inputs (map #(get-in assets %) inputs)
        requires (:requires view)]
    (assoc plan :queries (when requires
                           (apply requires view-inputs)))))

(s/defn ^:private ^:IO query->data :- (:data ViewPlan)
  [queries :- (:queries ViewPlan)]
  (fmap
   (fn [query?]
     (if (nil? (s/check c/Query? query?))
       (es/search query?)
       query?))
   queries))

(s/defn ^:private ^:IO queries->data :- ViewPlans
  [view-plans :- ViewPlans]
  (fmap
   (fn [{:keys [queries] :as plan}]
     (let [data (query->data queries)]
       (assoc plan :data data)))
   view-plans))

(s/defn ^:private keep-queries :- {ViewName {s/Keyword c/Query?}}
  [name-queries :- {ViewName {s/Keyword (s/either c/Query? Object)}}]
  (reduce
   (fn [outer [name queries]]
     (let [only-queries (reduce
                         (fn [inner [key value]]
                           (if (nil? (s/check c/Query? value))
                             (assoc inner key value)
                             inner
                             )
                           )
                         (array-map)
                         queries)]
       (if (seq only-queries)
         (assoc outer name only-queries)
         outer)))
   (array-map)
   name-queries))

(s/defn ^:private batch-queries->data :- ViewPlans
  [vps :- ViewPlans]
  (def vps'' vps)
  (let [name-queries (fmap :queries vps)
        ;; only pass-thru non-empty queries
        name-queries (into (array-map) (filter (fn [[n qs]] (seq qs)) name-queries))
        ;; remove static data to leave only queries
        only-queries (keep-queries name-queries)
        positions (c/flatten-map only-queries)
        query-batch (vals positions)
        data-batch (es/multi-search query-batch)
        position-paths (keys positions)
        ;; inject :data between the keys
        position-paths (map (fn [[a b]] [a :data b]) position-paths)
        ;; rejoin the paths with the data
        path-datas (c/zip position-paths data-batch)
        ;; copy the queries over to the data
        vps (fmap
             (fn [vp]
               (assoc vp :data (:queries vp)))
             vps)
        vps' (reduce
              (fn [acc [path data]]
                (assoc-in acc path data))
              vps
              path-datas)
        ]
    vps'))

(s/defn ^:private data->model :- ViewPlan
  [{:keys [view data] :as view-plan} :- ViewPlan]
  (assoc view-plan :model (c/expands view data)))

(s/defn ^:private models->html :- ViewPlans
  [view-plans :- ViewPlans]
  (reduce
   (fn [vps [key {:keys [name view model children] :as plan}]]
     (let [child-views (select-keys vps children)
           child-htmls (fmap :html child-views)
           html (c/renders view model child-htmls)]
       (assoc-in vps [name :html] html)))
   view-plans
   view-plans))

(s/defn ^:private ^:IO trigger-middlewares :- nil
  [middlewares step view-plans]
  (doseq [m middlewares]
    (m step view-plans)))

(s/defn ^:IO pipeline :- h/Html
  [page-tree  :- t/Tree
   views      :- {s/Keyword c/View?}
   app-config :- AppConfig
   request    :- http/Request]
  (let [middlewares [(fn [step {:keys [header] :as vps}]
                       (when (contains? #{:queries :data :model} step)
                         (do
                           (println (str "w/ " step))
                           (trace (-> vps :header)))
                         ))]
        trigger (partial trigger-middlewares middlewares)
        view-plans (page-tree->view-plans page-tree views)
        view-plans (fmap (partial require-queries app-config request) view-plans)
        _ (trigger :queries view-plans)
        view-plans (batch-queries->data view-plans)
        _ (trigger :data view-plans)
        view-plans (fmap data->model view-plans)
        _ (trigger :model view-plans)
        view-plans (models->html view-plans)
        _ (trigger :html view-plans)
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
