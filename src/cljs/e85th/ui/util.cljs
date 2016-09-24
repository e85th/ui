(ns e85th.ui.util
  (:require [goog.string :as gs]
            [goog.net.cookies]))

(defn nan?
  [x]
  (js/isNaN x))

(defn as-vector
  [x]
  (if (vector? x) x [x]))

(defn as-coll
  [x]
  (if (coll? x) x [x]))

(defn parse-int
  [s default]
  (let [v (js/parseInt s)]
    (if (nan? v) default v)))

(defn parse-float
  [s default]
  (let [v (js/parseFloat s)]
    (if (nan? v) default v)))

(defn event-value
  "reads the event target's value"
  [e]
  (-> e .-target .-value))

(defn event-checked
  "reads the event target's checked property"
  [e]
  (-> e .-target .-checked))

(defn key-event-value
  [e]
  (.-keyCode e))


(defn url-decode
  [s]
  (js/decodeURIComponent s))

(defn url-encode
  [s]
  (js/encodeURIComponent s))

(def format gs/format)

(defn unescape-html-entities
  [s]
  (gs/unescapeEntities s))

(defn cljs->json-string
  "Converts a cljs data structure to a json string."
  [cljs]
  (.stringify js/JSON (clj->js cljs)))

(defn json-string->cljs
  "Converts a json string to a cljs data structure."
  [json]
  (js->clj (.parse js/JSON json)))

(defn get-cookie-value
  "Gets the url decoded value of the cookie if it exists or nil."
  [name]
  (some-> (.get goog.net.cookies name) url-decode))

(defn set-cookie
  [name value max-age domain]
  (if (= "localhost" domain)
    (.set goog.net.cookies name value max-age "/")
    (.set goog.net.cookies name value max-age "/" domain)))

(defn remove-cookie
  [name]
  (.remove goog.net.cookies name))
