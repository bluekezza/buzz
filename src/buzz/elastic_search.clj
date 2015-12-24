(ns buzz.elastic-search
  (:import
   [org.elasticsearch.client.transport TransportClient])
  (:require
   [buzz.core :as c]
   [clojure.algo.generic.functor :refer [fmap]]
   [clojurewerkz.elastisch.native :as native]
   [clojurewerkz.elastisch.native.document :as doc]
   [clojurewerkz.elastisch.native.multi :as multi]
   [clojurewerkz.elastisch.native.response :as res]
   [clojurewerkz.elastisch.query :as q]
   [co.paralleluniverse.pulsar.core :as p]
   [schema.core :as s]))

(def strategies [:sequential
                 :multi
                 :parallel])

(def Strategy (apply s/enum strategies))

(def Index c/NonBlankStr)
(def Type c/NonBlankStr)
(def QueryMap c/Map)
(def From c/PosInt)
(def Size c/SPosInt)
(def Field c/NonBlankStr)
(def Fields (s/maybe [Field]))
(def Sort c/Map)

(def SingleQuery'
  {:index  Index
   :type   Type
   :query  QueryMap
   :from   From
   :size   Size
   :fields Fields
   :sort   (s/maybe c/Map)
   })

(s/defschema Data (-> c/Map
                      (s/named "Data")))

(s/defschema SingleQuery?
  "represents the parameters for an elastic search query"
  (s/both
   c/Query?
   (c/at-least
    {:index  Index
     :type   Type
     :query  QueryMap
     :from   From
     :size   Size
     :fields Fields
     :sort   (s/maybe Sort)})))

(defn extract
  [result]
  (->> result
       res/hits-from
       (map #(if (:_fields %) (:_fields %) (:_source %)))))

(s/defn ^:IO ^:private search* :- [Data]
  [conn :- TransportClient
   {:keys [index type query from size fields sort]} :- SingleQuery?]
  (-> (doc/search
       conn
       index
       type
       :query query
       :fields fields
       :from from
       :size size
       :sort sort)
      extract
      doall))

(s/defrecord SingleQuery [index  :- Index
                          type   :- Type
                          query  :- QueryMap
                          from   :- From
                          size   :- Size
                          fields :- Fields
                          sort   :- (s/maybe Sort)
                          ]
  c/Query
  (fetch ^:deprecated [self conn] (search* conn self)))

(s/defn for-multi-search
  [queries :- [SingleQuery?]]
  (->> (reduce
        (fn [acc query]
          (conj acc [(->> (select-keys query [:index :type])
                          (fmap name))
                     (select-keys query [:query :from :size :fields :sort])
                     ]))
        []
        queries)
       (apply concat)))

(s/defn ^:private ^:IO multi-search :- [[Data]]
  [conn :- TransportClient
   queries :- [SingleQuery?]]
  (let [multi-query (for-multi-search queries)
        multi-res (multi/search conn multi-query)
        results (->> multi-res
                     (map extract)
                     doall)]
    results))

#_(s/defn parallel-threads-search :- [[Data]]
  [conn :- TransportClient
   queries :- [SingleQuery?]]
  (let [futures (doall (map (fn [q] (future (search (conn q)))) queries))
        results (map deref futures)]
    results))

(s/defn ^:private ^:IO parallel-search :- [[Data]]
  [conn :- TransportClient
   queries :- [SingleQuery?]]
  (let [futures (doall (map (fn [q] (p/fiber->future (p/spawn-fiber #(search* conn q)))) queries))
        results (map deref futures)]
    results))

(s/defrecord MultiQuery [queries :- [SingleQuery]]
  c/Query
  (fetch [self conn] nil #_(multi-search queries)))

(s/defn ^:IO search :- [[Data]]
  [strategy :- Strategy
   conn :- TransportClient
   queries :- [SingleQuery?]]
  (let [strategy-fns {:sequential (fn [queries] (map #(search* conn %) queries))
                      :multi multi-search
                      :parallel parallel-search}
        strategy-fn (get strategy-fns strategy)]
    (strategy-fn conn queries)))

(def ^:private ServiceEndPoint [(s/one c/NetworkHost "Host") (s/one c/NetworkPort "Port")])

(s/defn ^:private url->endpoint :- ServiceEndPoint
  [url :- c/NonBlankStr]
  (let [[host port] (clojure.string/split url #":")]
    [host (Integer/parseInt port)]))

(s/defn ^:IO connect :- TransportClient
  [es-urls es-cluster]
  (let [timeout "10s"
        server-urls (clojure.string/split es-urls #",")
        server-endpoints (map url->endpoint server-urls)]
    (native/connect server-endpoints {"cluster.name" es-cluster
                                      "client.transport.ping_timeout" timeout})))
