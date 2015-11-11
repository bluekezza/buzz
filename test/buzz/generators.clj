(ns buzz.generators
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.rose-tree :as rose]
            [clojure.data.generators :as datgen]
            [clojure.test.check.clojure-test :refer (defspec)]
            [schema.core :as s]
            [com.gfredericks.test.chuck :as chuck]
            [clojure.algo.generic.functor :refer [fmap]]
            [buzz.core :as c]))

(def path
  (gen/such-that not-empty gen/string-alphanumeric))

(def Url
 {:protocol (s/enum "http" "https")
  :username s/Str
  :password s/Str
  :host     c/Str
  :port     s/Int
  :paths    [c/Str]
  :query    (s/maybe {c/Str c/Str})
  :anchor   s/Str})

(def url
  (gen/fmap
   (fn [{:keys [protocol username password host port paths query anchor]}]
     (let [user-pass (when (or username password) (str username ":" password "@"))]
       (str protocol "://" user-pass host "/" (apply str (interpose "/" paths)))))
   (gen/hash-map
    :protocol (gen/return "http")
    :username (gen/return nil)
    :password (gen/return nil)
    :host     (gen/return "www.buzz.co.uk")
    :port     (gen/return -1)
    :paths    (gen/vector path)
    :query    (gen/return nil)
    :anchor   (gen/return nil))))

(s/def ^:always-validate
  structure :- c/HICCUP
  "a description of the page tree"
  ["/" {:type :root}
   [["UK" {:type :region}
     [["education" {} []]
      ["media" {} []]
      ["society" {} []]
      ["law" {} []]
      ["scotland" {} []]
      ["wales" {} []]
      ["northern ireland" {} []]
      ]]
    ["world" {:type :region}
     [["europe" {} []]
      ["US" {} []]
      ["americas" {} []]
      ["asia" {} []]
      ["australia" {} []]
      ["africa" {} []]
      ["middle east" {} []]
      ["cities" {} []]
      ["development" {} []]
      ]]
    ["politics" {} []]
    ["sport" {}
     [["football" {} []]
      ["cricket" {} []]
      ["rugby union" {} []]
      ["F1" {} []]
      ["tennis" {} []]
      ["golf" {} []]
      ["cycling" {} []]
      ["boxing" {} []]
      ["racing" {} []]
      ["rugby league" {} []]
      ["US sports" {} []]
      ]]
    ["football" {}
     [["live scores" {} []]
      ["tables" {} []]
      ["competitions" {} []]
      ["results" {} []]
      ["fixtures" {} []]
      ["clubs" {} []]
      ]]
    ["opinion" {}
     [["columnists" {} []]
      ]]
    ["culture" {}
     [["film" {} []]
      ["tv & radio" {} []]
      ["music" {} []]
      ["games" {} []]
      ["books" {} []]
      ["art & design" {} []]
      ["stage" {} []]
      ["classical" {} []]
      ]]
    ["business" {}
     [["economics" {} []]
      ["banking" {} []]
      ["retail" {} []]
      ["markets" {} []]
      ["eurozone" {} []]
      ]]
    ["lifestyle" {}
     [["food" {} []]
      ["health & fitness" {} []]
      ["love & sex" {} []]
      ["family" {} []]
      ["women" {} []]
      ["home & garden" {} []]
      ]]
    ["fashion" {} []]
    ["environment" {}
     [["climate change" {} []]
      ["wildlife" {} []]
      ["energy" {} []]
      ["pollution" {} []]
      ]]
    ["tech" {} []]
    ["travel" {}
     [["UK" {} []]
      ["europe" {} []]
      ["US" {} []]
      ["skiing" {} []]
      ]]
    ]])
