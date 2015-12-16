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
  (fetch [self] "fetches the data"))

(s/defschema Query? (s/protocol Query))

(defprotocol View
  "a self-contained view"
  (requires [self cfg req] "given the initial seed works out what data it needs")
  (expands  [self data] "consumes the needed data and expands into its final form")
  (renders  [self model children] "View -> Any -> {s/Keyword Html} -> hiccup"))

(def View?
  (s/both (s/protocol View)
          {:inputs [PropertyPath]
           :requires (s/maybe (s/both (s/pred fn?)
                                      (s/make-fn-schema {s/Keyword Query} [[s/Any]])))}))

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
