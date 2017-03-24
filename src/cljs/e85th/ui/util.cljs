(ns e85th.ui.util
  (:require [goog.string :as gs]
            [clojure.string :as string]
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

(defn parse-bool
  ([x]
   (let [x (if (string? x)
             (string/trim (string/lower-case x))
             x)]
     (parse-bool x #{"true" "yes" "on" "1" 1 true})))
  ([x true-set]
   (some? (true-set x))))

(defn event-value
  "reads the event target's value"
  [e]
  (-> e .-target .-value))

(defn event-target-value
  "reads the event target's value"
  [e]
  (-> e .-target .-value))

(defn event-checked
  "reads the event target's checked property"
  [e]
  (-> e .-target .-checked))

(defn event-target-add-class
  [css-class e]
  (some-> e .-target .-classList (.add css-class)))

(defn event-target-rm-class
  [css-class e]
  (some-> e .-target .-classList (.remove css-class)))

(defn event-prevent-default
  [e]
  (.preventDefault e))

(defn event-stop-propogation
  [e]
  (.stopPropogation e))

(defn key-event-code
  [e]
  (.-keyCode e))

(defn key-event-value
  [e]
  (.-key e))


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


(defn set-window-location!
  [url]
  (set! js/window.location url))

(defn element-by-id
  [id]
  (js/document.getElementById id))

(defn element-exists?
  [id]
  (some? (element-by-id id)))

(defn element-value
  [id]
  (some-> id element-by-id .-value))

(defn iso-date-str->goog-date
  "Returns a goog.date.DateTime"
  [s]
  (goog.date.fromIsoString s))

(defn goog-date->iso-str
  [dt]
  (when dt
    (.toISOString (js/Date. (.getTime dt)))))


(defn feature-available?
  "object-name can be something like Notification or WebSocket as a string.
   Answers true if the feature is available."
  [object-name]
  (some? (aget js/window object-name)))


(def websockets-available? (partial feature-available? "WebSocket"))
(def notifications-available? (partial feature-available? "Notification"))

(defn prune-map
  "Prunes the map according to the "
  ([m]
   (prune-map m (fn [[k v]]
                  (or (nil? v)
                      (and (string? v)
                           (string/blank? v))))))
  ([m pred?]
   (into {} (remove pred? m))))


(defn stringify-json
  [x]
  (js/JSON.stringify x))

(defn selected-option-values
  "Answers with a seq of selected option values from an html select element."
  [dom-id]
  (reduce (fn [ans opt]
            (cond-> ans
              (.-selected opt) (conj (.-value opt))))
          []
          (some-> dom-id element-by-id .-options array-seq)))

(defn group-by+
  "Similar to group by, but allows applying val-fn to each item in the grouped by list of each key.
   Can also apply val-agg-fn to the result of mapping val-fn. All input fns are 1 arity.
   If val-fn and val-agg-fn were the identity fn then this behaves the same as group-by."
  ([key-fn val-fn xs]
   (group-by+ key-fn val-fn identity xs))
  ([key-fn val-fn val-agg-fn xs]
   (reduce (fn [m [k v]]
             (assoc m k (val-agg-fn (map val-fn v))))
           {}
           (group-by key-fn xs))))
