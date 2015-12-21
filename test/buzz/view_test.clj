(ns buzz.view-test
  (:require [buzz.core :as c]
            [buzz.core-test :as ct]
            [buzz.elastic-search :as es]
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

(deftest query->data-test
  (let [data-expected [{:id 1, :type :article}]
        queries {:articles (es/->SingleQuery "index"
                                             "type"
                                             {:match :articles}
                                             1
                                             nil)
                 :foo :bar}
        data-actual (with-redefs [es/search (fn [query] data-expected)]
                      (#'v/query->data queries))]
    (is (= {:articles data-expected
            :foo :bar}
           data-actual))))

(defn ->ViewPlan
  [name view queries]
  {:name     name
   :view     view
   :children []
   :queries  queries
   :data     nil
   :model    nil
   :html     nil})

(deftest keep-queries-test
  (let [header-queries {:articles (es/->SingleQuery "buzz" "articles" {:query :articles} 1 nil)
                        :videos (es/->SingleQuery  "buzz" "videos" {:query :videos} 1 nil)
                        :foo :bar}
        footer-queries {:topics (es/->SingleQuery  "buzz" "topics" {:query :topics} 1 nil)
                        :fuzz :buzz}
        name-queries {:header header-queries
                      :footer footer-queries}
        expected {:header (select-keys header-queries [:articles :videos])
                  :footer (select-keys footer-queries [:topics])}
        actual (#'v/keep-queries name-queries)]
    (is (= expected actual))))

(deftest batch-queries-data-test
  (let [articles [{:id 1, :type :article} {:id 2, :type :article}]
        videos [{:id 21 :type :video} {:id 22 :type :video}]
        topics [{:id 11, :type :topic} {:id 12, :type :topic}]
        header-queries {:articles (es/->SingleQuery "buzz" "articles" {:query :articles} 1 nil)
                        :videos (es/->SingleQuery "buzz" "videos" {:query :videos} 1 nil)
                        :foo :bar}
        header-data (assoc header-queries :articles articles :videos videos)
        header-view (ct/mkMockView header-queries)
        footer-queries {:topics (es/->SingleQuery "buzz" "topics" {:query :topics} 1 nil)
                        :fuzz :buzz}
        footer-data (assoc footer-queries :topics topics)
        footer-view (ct/mkMockView footer-queries)
        vps {:header (->ViewPlan :header header-view header-queries)
             :footer (->ViewPlan :footer footer-view footer-queries)}
        vps-expected (-> vps
                         (assoc-in [:header :data] header-data)
                         (assoc-in [:footer :data] footer-data))
        ;; with only one pass of the batcher
        vps-actual (with-redefs
                     [es/multi-search (fn [qs]
                                        (map
                                         (fn [q]
                                           (case (:type q)
                                             "articles" articles
                                             "videos"   videos
                                             "topics"   topics))
                                         qs))]
                     (#'v/batch-queries->data vps))]
    (is (= vps-expected vps-actual))))
