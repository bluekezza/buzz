(ns buzz.endpoint.example
  (:require [compojure.core :refer :all]
            [clojure.java.io :as io]))

(def welcome-page
  (io/resource "buzz/endpoint/example/welcome.html"))

(defn example-endpoint [{{db :spec} :db}]
  (routes
   (GET "/" [] welcome-page)))
