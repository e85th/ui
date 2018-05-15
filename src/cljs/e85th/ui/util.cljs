(ns e85th.ui.util
  (:require [goog.string :as gs]
            [clojure.string :as str]))

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
             (str/trim (str/lower-case x))
             x)]
     (parse-bool x #{"true" "yes" "on" "1" 1 true})))
  ([x true-set]
   (some? (true-set x))))


(defn url-decode
  [s]
  (js/decodeURIComponent s))

(defn url-encode
  [s]
  (js/encodeURIComponent s))


(defn params->query-string
  [m]
  (str/join "&" (for [[k v] m]
                  (str (name k) "=" (url-encode v)))))

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

(defn iso-date-str->goog-date
  "Returns a goog.date.DateTime"
  [s]
  (goog.date.fromIsoString s))

(defn goog-date->iso-str
  [dt]
  (when dt
    (.toISOString (js/Date. (.getTime dt)))))

(defn prune-map
  "Prunes the map according to the "
  ([m]
   (prune-map m (fn [[k v]]
                  (or (nil? v)
                      (and (string? v)
                           (str/blank? v))))))
  ([m pred?]
   (into {} (remove pred? m))))


(defn stringify-json
  [x]
  (js/JSON.stringify x))

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

(defn assoc-in+
  "Similar to assoc-in except can specify multiple kv pairs"
  [m & path-vals]
  (assert (even? (count path-vals)) "Expected even number of paths and values")
  (reduce (fn [m [path v]]
            (assoc-in m path v))
          m
          (partition 2 path-vals)))
