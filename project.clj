(defproject e85th/ui "0.1.14"
  :description "Frontend UI code."
  :url "https://github.com/e85th/ui"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [re-frame "0.8.0"]
                 [com.taoensso/sente "1.11.0"] ; websockets
                 [prismatic/schema "1.1.2"]
                 [funcool/hodgepodge "0.1.4"] ;; local storage
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 ;; for development
                 [devcards "0.2.1-7"]
                 [cljs-ajax "0.5.8"]
                 [kioo "0.4.2"]
                 [com.taoensso/timbre "4.7.4"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :cljsbuild {:builds [{:source-paths ["src/cljs" "src/cljc"]}]}

  :plugins [[codox "0.8.13"]
            [test2junit "1.1.2"]
            [lein-cljsbuild "1.1.3"]]

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
