(defproject e85th/ui "0.1.39-alpha2"
  :description "Frontend UI code."
  :url "https://github.com/e85th/ui"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2" :scope "provided"]
                 [org.clojure/clojurescript "1.9.660" :scope "provided"]
                 [com.google.guava/guava "23.1-jre"]
                 [org.clojure/core.async "0.3.443"] ;; override timbre/sente version for spec ns
                 [com.taoensso/timbre "4.10.0"]
                 [e85th/commons "0.1.29-alpha3"]
                 [re-frame "0.10.2"]
                 [com.cemerick/url "0.1.1"]
                 [com.taoensso/sente "1.11.0"] ; websockets
                 [funcool/hodgepodge "0.1.4"] ;; local storage
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [hipo "0.5.2"] ;; hiccup -> dom modifiable item (for working with other js libs)
                 ;; for development
                 [devcards "0.2.4"]
                 [cljs-ajax "0.7.2"]
                 [kioo "0.5"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :cljsbuild {:builds [{:source-paths ["src/cljs" "src/cljc"]}]}

  :plugins [[codox "0.8.13"]
            [test2junit "1.1.2"]
            [lein-cljsbuild "1.1.6"]]

  :deploy-repositories [["releases"  {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])
