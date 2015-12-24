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
            [buzz.core :as c]
            [cemerick.url :as u]))

(def path
  (gen/such-that not-empty gen/string-alphanumeric))

(def Url
 {:protocol (s/enum "http" "https")
  :username s/Str
  :password s/Str
  :host     c/NonBlankStr
  :port     s/Int
  :paths    [c/NonBlankStr]
  :query    (s/maybe {c/NonBlankStr c/NonBlankStr})
  :anchor   s/Str})

(def url-map
  (gen/hash-map
   :protocol (gen/elements ["http" "https"])
   :username (gen/return nil)
   :password (gen/return nil)
   :host     (gen/return "www.buzz.co.uk")
   :port     (gen/return -1)
   :paths    (gen/vector path)
   :query    (gen/return nil)
   :anchor    gen/string-alphanumeric))

(defn url-map-to-str
  [{:keys [protocol username password host port paths query anchor]}]
  (let [user-pass (when (or username password) (str username ":" password "@"))
        query-str (when query (str "?" query))
        anchor-str (when anchor (str "#" anchor))]
    (apply str (concat
                [protocol
                 "://"
                 user-pass
                 host
                 "/"]
                (interpose "/" paths)
                [anchor-str query-str]))))

(def url (gen/fmap url-map-to-str url-map))

(defspec all-generated-urls-are-valid
  50
  (prop/for-all [url' url]
    (u/url url')))
