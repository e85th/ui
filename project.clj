(defproject e85th/ui "0.1.35"
  :description "Frontend UI code."
  :url "https://github.com/e85th/ui"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]
                 [org.clojure/clojurescript "1.9.660" :scope "provided"]
                 [org.clojure/core.async "0.3.443"] ;; override timbre/sente version for spec ns
                 [com.taoensso/timbre "4.10.0"]
                 [re-frame "0.9.4"]
                 [com.cemerick/url "0.1.1"]
                 [com.taoensso/sente "1.11.0"] ; websockets
                 [funcool/hodgepodge "0.1.4"] ;; local storage
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [hipo "0.5.2"] ;; hiccup -> dom modifiable item (for working with other js libs)
                 ;; for development
                 [devcards "0.2.3"]
                 [cljs-ajax "0.6.0"]
                 [kioo "0.5"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :cljsbuild {:builds [{:source-paths ["src/cljs" "src/cljc"]}]}

  :plugins [[codox "0.8.13"]
            [test2junit "1.1.2"]
            [lein-cljsbuild "1.1.6"]]

  :deploy-repositories [["releases"  {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])
