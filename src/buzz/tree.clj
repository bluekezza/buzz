(ns buzz.tree
  (:refer-clojure :exclude [pop])
  (:require [schema.core :as s]))

(def Tree
  "the schema for a Tree"
  [(s/one s/Int "value")
   (s/one [(s/recursive #'Tree)] "children")])

(s/defn children :- [Tree]
  "returns the children of the tree node"
  [t :- Tree]
  (second t))

(s/defn value-of :- s/Int
  "returns the value-of the tree node"
  [t :- Tree]
  (first t))

(s/defn reverse-level-order :- [s/Int]
  "traverses the tree in reverse level order"
  [root :- Tree]
  (let [pop (fn [q] [(first q) (vec (rest q))])]
    (loop [queue [root]
           stack '()]
      (if (empty? queue)
        stack
        (let [[n queue] (pop queue)
              queue (apply conj queue (reverse (children n)))
              stack (conj stack (value-of n))]
          (recur queue stack))))))
