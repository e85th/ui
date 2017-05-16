(ns e85th.ui.browser
  (:require [goog.net.cookies]
            [e85th.ui.util :as u]
            [cemerick.url :as url]
            [taoensso.timbre :as log]
            [clojure.walk :as walk]))

(defn get-cookie-value
  "Gets the url decoded value of the cookie if it exists or nil."
  [name]
  (some-> (.get goog.net.cookies name) u/url-decode))

(defn set-cookie
  [name value max-age domain]
  (if (= "localhost" domain)
    (.set goog.net.cookies name value max-age "/")
    (.set goog.net.cookies name value max-age "/" domain)))

(defn rm-cookie
  [name]
  (.remove goog.net.cookies name))

(defn feature-available?
  "object-name can be something like Notification or WebSocket as a string.
   Answers true if the feature is available."
  [object-name]
  (some? (aget js/window object-name)))


(def websockets-available? (partial feature-available? "WebSocket"))
(def notifications-available? (partial feature-available? "Notification"))


(defn location
  "Gets or sets the browser location"
  ([]
   (.-location js/window))
  ([url]
   (set! js/window.location url)))

(defn href
  []
  (.-href (location)))

(defn origin
  []
  (.-origin (location)))

(defn pathname
  []
  (.-pathname (location)))

(defn search
  []
  (.-search (location)))

(defn query-params
  ([]
   (query-params (href)))
  ([url]
   (walk/keywordize-keys (:query (url/url url)))))
