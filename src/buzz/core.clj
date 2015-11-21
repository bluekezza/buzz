(ns buzz.core
  (:require [schema.core :as s]))

(defmacro functionize [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defmacro apply-macro [macro args]
  `(apply (functionize ~macro) ~args))

(def PosInt (s/both s/Int (s/pred #(> % 0))))
(def Str (s/both s/Str (s/pred #(not (clojure.string/blank? %)))))
(def Map (s/either clojure.lang.PersistentHashMap clojure.lang.PersistentArrayMap))

(defn schema-for-protocol
  [type]
  (s/pred #(and (not (nil? %)) (satisfies? type %))))

(defprotocol View
  "a self-contained view"
  (requires [self cfg req] "given the initial seed works out what data it needs")
  (expands  [self data] "consumes the needed data and expands into its final form")
  (renders  [self model children] "renders structured hiccup html"))

(def View? (schema-for-protocol View))

(defn fetch
  "a placeholder for fetching queries"
  [query]
  nil)
