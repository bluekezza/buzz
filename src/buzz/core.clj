(ns buzz.core
  (:require [schema.core :as s]))

(defmacro functionize [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defmacro apply-macro [macro args]
  `(apply (functionize ~macro) ~args))

(def PosInt (s/both s/Int (s/pred #(> % 0))))
(def Str (s/both s/Str (s/pred #(not (clojure.string/blank? %)))))
(def Map (s/either clojure.lang.PersistentHashMap clojure.lang.PersistentArrayMap))
(def PropertyPath [(s/either s/Keyword s/Int)])

(defn schema-for-protocol
  [type]
  (s/pred #(and (not (nil? %)) (satisfies? type %))))

(defprotocol Query
  "the protocol for a query"
  (fetch [self] "fetches the query"))

(def Query? (s/protocol Query))

(defprotocol View
  "a self-contained view"
  (expands  [self data] "consumes the needed data and expands into its final form")
  (renders  [self model children] "View -> Any -> {s/Keyword Html} -> hiccup"))

(defn at-least
  "validates that at least the keys specified are present"
  [MapSchema]
  (s/pred
    (fn [v]
      (let [error (s/check MapSchema (select-keys v (keys MapSchema)))]
        (not (boolean error))))))

(def View?
  (s/both (s/protocol View)
          (at-least
           {:inputs [PropertyPath]
            :requires (s/maybe (s/both (s/pred fn?)
                                       (s/make-fn-schema {s/Keyword Query} [[s/Any]])))})))

(defprotocol Page
  "a page"
  (routes [self] "routes link to pages"))

(s/defschema Page? (s/protocol Page))

(defn str'
  "a faster str implementation using a mutable string builder"
  [& seq-of-strings]
  (loop [buffer (StringBuilder.)
         args seq-of-strings]
    (if-not (seq args)
      (str buffer)
      (recur (.append buffer (first args)) (rest args)))))

(defn zip
  [as bs]
  (map vector as bs))

(defn nat
  "natural numbers"
  []
  (iterate inc 0))

(s/defn nest-map
  "nests all the keys of the map under the given name: n"
  [n :- s/Keyword
   mp :- Map]
  (reduce-kv
   (fn [acc k v]
     (assoc acc [n k] v)
     )
   {}
   mp))

(defn flatten-map
  [map']
  (->> (reduce
       (fn [acc [k m]]
         (let [m' (nest-map k m)]
           (conj acc m')))
       []
       map')
       (apply concat)
       (into (array-map))))
