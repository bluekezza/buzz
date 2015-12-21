(ns buzz.elastic-search
  (:require
   [buzz.core :as c]
   [clojure.algo.generic.functor :refer [fmap]]
   [schema.core :as s]))

(def Index s/Str)
(def Type s/Str)
(def QueryMap c/Map)
(def Size c/PosInt)
(def Field s/Keyword)
(def Fields (s/maybe [Field]))

(def SingleQuery'
  {:index  Index
   :type   Type
   :query  QueryMap
   :size   Size
   :fields Fields})

(s/defschema SingleQuery?
  "represents the parameters for an elastic search query"
  (s/both
   c/Query?
   (c/at-least {:index  Index
                :type   Type
                :query  QueryMap
                :size   Size
                :fields Fields})))

(s/defn search :- [s/Any]
  [query :- SingleQuery?]
  nil)

(s/defrecord SingleQuery [index  ;- Index
                          type   ;- Type
                          query  ;- Query
                          size   ;- Size
                          fields ;- Fields
                          ]
  c/Query
  (fetch [self] (search self)))

(s/defn for-multi-search
  [queries :- [SingleQuery?]]
  (->> (reduce
        (fn [acc query]
          (conj acc [(->> (select-keys query [:index :type])
                          (fmap name))
                     (select-keys query [:query :size :fields])
                     ]))
        []
        queries)
       (apply concat)))

(s/defn multi-search :- [s/Any]
  [queries :- [c/Query?]]
  ;; (doc/multi-search client conn (for-multi-search queries))
  nil)

(s/defrecord MultiQuery [queries :- [SingleQuery]]
  c/Query
  (fetch [self] (multi-search queries)))
