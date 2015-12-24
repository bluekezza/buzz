(ns buzz.view
  (:import
   [clojure.lang ExceptionInfo]
   [org.elasticsearch.client.transport TransportClient])
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


(s/defschema QueriesAndData {s/Keyword (s/either c/Query? Object)})

(s/defschema Queries {s/Keyword c/Query?})

(s/defschema ViewName (-> s/Keyword
                          (s/named "ViewName")))

(s/defschema ViewPlan
  {:name      ViewName
   :view      c/View?
   :children [s/Keyword]
   :queries   (s/maybe QueriesAndData)
   :data      (s/maybe {s/Keyword (s/either es/Data Object)})
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

(s/defn ^:private queries->data :- ViewPlans
  [search :- (s/make-fn-schema [[es/Data]] [[es/SingleQuery?]])
   vps :- ViewPlans]
  (let [;; only pass-thru non-empty queries
        named-queries-or-data (->> vps                      ;- (c/ArrayMap ViewName ViewPlan)
                                   (fmap :queries)          ;- (c/ArrayMap ViewName (s/maybe QueriesAndData})
                                   (filter
                                    (fn [[n qs]]
                                      (seq qs)))
                                   (into (array-map)))      ;- (c/ArrayMap ViewName QueriesAndData)
        named-queries (keep-queries named-queries-or-data)  ;- (c/ArrayMap ViewName Queries)
        positions (c/flatten-map named-queries)             ;- (c/ArrayMap Path c/Query?)
        ;; these positions will track the path the query came from, and will be used to calculate the path to assoc to
        query-list (vals positions)                         ;- [es/SingleQuery?]
        data-list (search query-list)                       ;- [[es/Data]]
        position-paths (->> positions                       ;- (c/ArrayMap Path c/Query?)
                            keys                            ;- [Path]
                            (map (fn [[a b]]
                                   [a :data b])))           ;- [Path]
        ;; position-paths now refers to the position to assoc-in the data into the ViewPlan
        paths-and-data (c/zip position-paths data-list)     ;- [[(a Path) (a Data)]]
        ;; data is now rejoined with their paths
        vps (fmap
             (fn [vp]
               (assoc vp :data (:queries vp)))
             vps)
        ;; ViewPlans now have their queries copied into their data slots
        vps' (reduce
              (fn [acc [path data]]
                (assoc-in acc path data))
              vps
              paths-and-data)
        ;; ViewPlans now have their es/Data assoc'd into the correct positions
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
  [search :- (s/make-fn-schema [[es/Data]] [[es/SingleQuery?]])
   page-tree  :- t/Tree
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
        view-plans (queries->data search view-plans)
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
