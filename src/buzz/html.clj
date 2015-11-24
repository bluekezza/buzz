(ns buzz.html
  (:import [clojure.lang ExceptionInfo])
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

(s/defn element
  [tag-name :- s/Keyword]
  (letfn [(element [attrs elems]
            {:tag tag-name
             :type :element
             :attrs attrs
             :content (cond
                       (nil? elems)
                       []
                       (and (sequential? elems)
                            (sequential? (first elems)))
                       (first elems)
                       :else
                       (vec elems))})]
    (fn
      ([]
       (element {} []))
      ([attrs & elems]
       (when-not (map? attrs) (throw (ExceptionInfo. "first argument must be a map" attrs)))
       (element attrs elems)))))

(def html (element :html))
(def head (element :head))
(def body (element :body))
(def div (element :div))
(def span (element :span))
(def a (element :a))
(def img (element :img))
(def ul (element :ul))
(def li (element :li))
(def script (element :script))
(def strong (element :strong))
(def b (element :b))

(s/defn as-hickory :- Hickory
  [hiccup :- Hiccup]
  (let [convert #(-> (hickory.convert/hiccup-to-hickory %)
                     :content first :content second :content)]
    (if-not (sequential? (first hiccup))
      (first (convert [hiccup]))
      (convert hiccup))))
