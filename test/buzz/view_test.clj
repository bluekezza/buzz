(ns buzz.view-test
  (:require [buzz.core :as c]
            [buzz.html :refer :all]
            [buzz.tree :as t]
            [buzz.view :as v]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as e]
            [schema.core :as s]))

(s/set-fn-validation! true)

(deftest from-hiccup-test
  (is (= [{:type :element, :attrs {:class "footer"}, :tag :div, :content ["footer"]}]
         (from-hiccup [[:div {:class "footer"} "footer"]]))))

(deftest from-hiccup-test-2
  (is (= {:type :element, :attrs {:class "footer"}, :tag :div, :content ["footer"]}
         (from-hiccup [:div {:class "footer"} "footer"]))))

(s/defrecord ^:private Header
  [inputs requires]
  c/View
  (expands [this data] nil)
  (renders [this model children]
    (div {:class "header"} "header")))

(s/defn mkHeader  :- c/View?
  []
  (->Header nil nil))

(s/defrecord ^:private Content
  [inputs requires]
  c/View
  (expands [this data] nil)
  (renders [this model children]
    (div {:class "content"} "content")))

(s/defn mkContent :- c/View?
  []
  (->Content nil nil))

(s/defrecord ^:private Footer
  [inputs requires]
  c/View
  (expands [this data] nil)
  (renders [this model children]
    (from-hiccup
      [:div {:class "footer"} "footer"])))

(s/defn mkFooter :- c/View?
  []
  (->Footer nil nil))

(s/defrecord ^:private Page
  [inputs requires]
  c/View
  (expands [this data] nil)
  (renders [this model children]
    (html {}
      (head {})
      (body {}
        (div {:class "adbanner"})
        (div {:class "page"}
          (vals children))))))

(s/defn mkPage :- c/View?
  []
  (->Page nil nil))

(s/def views :- {s/Keyword c/View?}
  {:header  (mkHeader)
   :content (mkContent)
   :footer  (mkFooter)
   :page    (mkPage)})

(s/def page :- t/Tree
  (let [h [:header []]
        c [:content []]
        f [:footer []]
        p [:page [h c f]]]
    p))

(s/def render-tree :- Html
   {:tag :html
    :type :element
    :attrs {}
    :content
    [{:tag :head
      :type :element
      :attrs {}
      :content []}
     {:tag :body
      :type :element
      :attrs {}
      :content
      [{:tag :div
        :type :element
        :attrs {:class "adbanner"}
        :content []}
       {:tag :div
        :type :element
        :attrs {:class "page"}
        :content
        [{:tag :div, :type :element, :attrs {:class "header"}, :content ["header"]}
         {:tag :div, :type :element, :attrs {:class "content"}, :content ["content"]}
         {:tag :div, :type :element, :attrs {:class "footer"}, :content ["footer"]}]}]}]})

(deftest pipeline-test
  (is (= render-tree (v/pipeline page views {} {}))))

(s/def render-html-str :- s/Str
  "<html><head></head><body><div class=\"adbanner\"></div><div class=\"page\"><div class=\"header\">header</div><div class=\"content\">content</div><div class=\"footer\">footer</div></div></body></html>")

(deftest html-test-2
  (is (= render-html-str
         (v/html-as-str {:tag :html,
                         :type :element,
                         :attrs {},
                         :content
                         [{:tag :head, :type :element, :attrs {}, :content []}
                          {:tag :body,
                           :type :element,
                           :attrs {},
                           :content
                           [{:tag :div, :type :element, :attrs {:class "adbanner"}, :content []}
                            {:tag :div,
                             :type :element,
                             :attrs {:class "page"},
                             :content
                             [{:tag :div, :type :element, :attrs {:class "header"}, :content ["header"]}
                              {:tag :div, :type :element, :attrs {:class "content"}, :content ["content"]}
                              {:tag :div,
                               :type :element,
                               :attrs {:class "footer"},
                               :content ["footer"]}]}]}]}))))

(deftest html-test-3
  (let [html-data [{:type :dtd,
                    :data
                    ["html"
                     "-//W3C//DTD XHTML 1.0 Transitional//EN"
                     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"]}
                   {:tag :html,
                    :attrs {:xml:lang "en", :lang "en"},
                    :content
                    ["\n  "
                     {:tag :head, :attrs nil, :content ["\n  "]}
                     "\n  "
                     {:tag :body,
                      :attrs {:id "home", :class "homehome mol-desktop "},
                      :content ["\n  "]}
                     "\n\n"]}]
        html-str-expected "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xml:lang=\"en\" lang=\"en\">\n  <head>\n  </head>\n  <body id=\"home\" class=\"homehome mol-desktop \">\n  </body>\n\n</html>"
        ]
    (is (= html-str-expected (v/html-as-str html-data)))))

(deftest html-test
  (is (= render-html-str
         (v/html-as-str (v/pipeline page views {} {})))))

(e/defsnippet child "buzz/child.html" [:div] [])

(e/defsnippet parent "buzz/parent.html" [:div] [])

(deftest transform-by-id-test
  (is (= [{:tag :div, :attrs {:id "child", :class "child-style"}
           :content ["\n  " {:tag :span, :attrs nil, :content ["cheese"]} "\n"]}]
         (v/transform-by-id (parent) {"child" (child)}))))

(defrecord MockQuery [data]
  c/Query
  (fetch [self] data))

(deftest query->data-test
  (let [data-expected [{:dummy :data}]
        queries {:articles (->MockQuery data-expected)
                 :foo :bar}
        data-actual (#'v/query->data queries)]
    (is (= {:articles data-expected
            :foo :bar}
           data-actual))))
