(ns buzz.http
  (:require
   [buzz.core :as c]
   [schema.core :as s]))

(s/defschema Request c/Map)
