(ns buzz.core
  (:require [schema.core :as s]))

(def PosInt (s/both s/Int (s/pred #(> % 0))))
(def Str (s/both s/Str (s/pred #(not (clojure.string/blank? %)))))
(def Map (s/either clojure.lang.PersistentHashMap clojure.lang.PersistentArrayMap))
(def HICCUP
  [(s/one (s/either Str s/Keyword) "name")
   (s/one Map "attributes")
   (s/one [(s/recursive #'HICCUP)] "children")])
