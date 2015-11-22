(ns buzz.html
  (:require [buzz.core :as c]
            [hickory.convert]
            [schema.core :as s]))

(def Hiccup
  "placeholder for a hiccup schema"
  s/Any)

(def Hickory (s/either c/Map [s/Any]))

(defn document
  [& elems]
  {:type :document
   :content (vec elems)})

(defn html
  ([attrs & elems]
   {:tag :html
    :type :element
    :attrs nil
    :content (vec elems)}))

(defn head
  ([attrs & elems]
   {:tag :head
    :type :element
    :attrs attrs
    :content (vec elems)}))

(defn body
  ([attrs & elems]
   {:tag :body
    :type :element
    :attrs attrs
    :content (vec elems)}))

(defn div
  [attrs & elems]
  {:tag     :div
   :type    :element
   :attrs   attrs
   :content (if (and (sequential? elems)
                     (sequential? (first elems)))
              (first elems)
              elems)})

(s/defn as-hickory :- Hickory
  [hiccup :- Hiccup]
  (let [convert #(-> (hickory.convert/hiccup-to-hickory %)
                     :content first :content second :content)]
    (if-not (sequential? (first hiccup))
      (first (convert [hiccup]))
      (convert hiccup))))
