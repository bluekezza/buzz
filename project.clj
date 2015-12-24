(defproject buzz "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[clojurewerkz/elastisch "2.1.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.stuartsierra/component "0.3.0"]
                 [compojure "1.4.0"]
                 [duct "0.4.5"]
                 [duct/hikaricp-component "0.1.0"]
                 [enlive "1.1.6"]
                 [environ "1.0.1"]
                 [hiccup "1.0.5"]
                 [hickory "0.5.4"]
                 [meta-merge "0.1.1"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/clojure "1.7.0"]
                 [org.postgresql/postgresql "9.4-1203-jdbc4"]
                 [org.slf4j/slf4j-nop "1.7.12"]
                 [org.webjars/normalize.css "3.0.2"]
                 [prismatic/schema "1.0.3"]
                 [ring "1.4.0"]
                 [ring-jetty-component "0.3.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [co.paralleluniverse/quasar-core "0.7.3"]
                 [co.paralleluniverse/pulsar "0.7.3"]
                 ]
  :java-agents [[co.paralleluniverse/quasar-core "0.7.3"]]
  :plugins [[lein-environ "1.0.1"]
            [lein-gen "0.2.2"]]
  :generators [[duct/generators "0.4.5"]]
  :duct {:ns-prefix buzz}
  :main ^:skip-aot buzz.main
  :target-path "target/%s/"
  :aliases {"gen"   ["generate"]
            "setup" ["do" ["generate" "locals"]]}
  :profiles
  {:dev  [:project/dev  :profiles/dev]
   :test [:project/test :profiles/test]
   :uberjar {:aot :all}
   :profiles/dev  {}
   :profiles/test {}
   :project/dev   {:source-paths ["dev"]
                   :repl-options {:init-ns user}
                   :dependencies [[com.gfredericks/test.chuck "0.2.1"]
                                  [eftest "0.1.0"]
                                  [kerodon "0.7.0"]
                                  [org.clojure/data.generators "0.1.2"]
                                  [org.clojure/test.check "0.8.2"]
                                  [org.clojure/test.generative "0.5.2"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [reloaded.repl "0.2.1"]]
                   :env {:port 3000}}
   :project/test  {}})
