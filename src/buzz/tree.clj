(ns buzz.tree
  (:refer-clojure :exclude [pop])
  (:require [schema.core :as s]))

(def Tree
  "the schema for a Tree"
  [(s/one s/Any "value")
   (s/one [(s/recursive #'Tree)] "children")])

(s/defn children-of :- [Tree]
  "returns the children of the tree node"
  [t :- Tree]
  (second t))

(s/defn value-of :- s/Any
  "returns the value-of the tree node"
  [t :- Tree]
  (first t))

(s/defn reverse-level-order-walk :- [s/Any]
  "traverses the tree in reverse level order"
  [f    :- (s/make-fn-schema s/Any [Tree])
   root :- Tree]
  (let [pop (fn [q] [(first q) (vec (rest q))])]
    (loop [queue [root]
           stack '()]
      (if (empty? queue)
        stack
        (let [[n queue] (pop queue)
              queue (apply conj queue (reverse (children-of n)))
              stack (conj stack (f n))]
          (recur queue stack))))))

(s/defn reverse-level-order :- [s/Any]
  "traverses the tree in reverse level order"
  [root :- Tree]
  (reverse-level-order-walk value-of root))
