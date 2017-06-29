(set-env!
 :resource-paths #{"src/clj" "src/cljs" "src/cljc" "resources"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]
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
                 [kioo "0.5"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [adzerk/boot-cljs "2.0.0" :scope "test"]]

 :repositories #(conj %
                      ["clojars" {:url "https://clojars.org/repo"
                                  :username (System/getenv "CLOJARS_USER")
                                  :password (System/getenv "CLOJARS_PASS")}]))

(require '[adzerk.boot-test :as boot-test])
(require '[adzerk.boot-cljs :refer [cljs]])

(deftask test
  "Runs the unit-test task"
  []
  (comp
   (javac)
   (cljs)
   (boot-test/test)))



(deftask build
  "Builds a jar for deployment."
  []
  (comp
   (javac)
   (pom)
   (jar)
   (target)))

(deftask dev
  "Starts the dev task."
  []
  (comp
   (repl)
   (watch)))

(deftask deploy
  []
  (comp
   (build)
   (push)))

(task-options!
 pom {:project 'e85th/ui
      :version "0.1.29"
      :description "UI code."
      :url "http://github.com/e85th/ui"
      :scm {:url "http://github.com/e85th/ui"}
      :license {"Apache License 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}}
 push {:repo "clojars"})
