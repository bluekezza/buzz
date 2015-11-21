(ns buzz.view
  (:require
   [buzz.core :as c]
   [buzz.tree :as t]
   [schema.core :as s]))

(def Hiccup
  "placeholder for a hiccup schema"
  s/Any)

(s/defn pipeline :- Hiccup
  [page-tree :- t/Tree
   views     :- {s/Keyword c/View?}]
  (let [view-order (t/reverse-level-order page-tree)
        view-queries (map
                      (fn [view-key]
                        (let [view (get views view-key)]
                          [view-key (c/requires view nil nil)]))
                      view-order)
        view-datas (map
                    (fn [[view-key query]]
                      (let [data (c/fetch query)]
                        [view-key data]))
                    view-queries)
        view-models (map
                     (fn [[view-key data]]
                       (let [view (get views view-key)]
                         [view-key (c/expands view data)]))
                     view-datas)
        views-rendered (reduce
                          (fn [children [view-key model]]
                            (let [view (get views view-key)
                                  html (c/renders view model children)]
                              (assoc children view-key html)))
                          {}
                          view-models)]
    (:page views-rendered)))

(s/defn ->html :- s/Str
  [h :- Hiccup]
  (c/apply-macro hiccup.page/html5 h))
