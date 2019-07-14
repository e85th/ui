(ns e85th.ui.rf.inputs
  (:refer-clojure :exclude [select])
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom :refer [RAtom Reaction RCursor Track Wrapper]]
            [taoensso.timbre :as log]
            [e85th.ui.dom :as dom]
            [e85th.ui.time :as time]
            [e85th.ui.moment :as moment]
            [e85th.ui.util :as u]
            [taoensso.timbre :as log]
            [goog.events :as events]
            [clojure.string :as str]
            [clojure.set :as set])
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]
           [goog.date Date DateTime]))


(defn dispatch-event
  [rf-event event-value & args]
  (rf/dispatch (into (u/as-vector rf-event) (cons event-value args))))

(defn event-handler
  ([rf-event]
   (fn [e]
     (rf/dispatch (u/as-vector rf-event))))
  ([rf-event event-reader-fn]
   (fn [e]
     (dispatch-event rf-event (event-reader-fn e)))))

(defn rename-class-attr
  "attrs is a map of attrs from kioo"
  [{:keys [className] :as attrs}]
  (-> attrs
      (assoc :class className)
      (dissoc :className)))


(defn classes->class
  "Converts a collection of classes (strings/keywords) to 1 string
  used for the :class key in hiccup."
  [classes]
  (str/join " " classes))

(defn compute-class
  "Computes the value of the class attr based on existing value
   and classes being added and removed"
  ([css-class classes-to-add]
   (compute-class css-class classes-to-add nil))
  ([css-class classes-to-add classes-to-rm]
   (let [existing-classes (-> (or css-class "")
                              (str/split #" ")
                              set)]
     (->> (set/difference existing-classes (set classes-to-rm))
          (set/union (set classes-to-add))
          (classes->class)))))

(defn assoc-class
  [{:keys [class] :as attrs} {:keys [+classes -classes =classes]}]
  (assoc attrs
         :class (cond
                  =classes (classes->class =classes)
                  (or +classes -classes) (compute-class class +classes -classes)
                  :else class)))


(defn- ensure-simple-content
  [node]
  (let [content (or (first (:content node [""]))
                    "")]
    (when-not (string? content)
      (throw (ex-info "Can only deal with simple string content." node)))
    content))

(defn deref-or-value-peek
  "Takes a value or an atom
  If it's a value, returns it
  If it's a Reagent object that supports IDeref, returns the value inside it, but WITHOUT derefing
  The arg validation code uses this, since calling deref-or-value adds this arg to the watched ratom list for the component
  in question, which in turn can cause different rendering behaviour between dev (where we validate) and prod (where we don't).
  This was experienced in popover-content-wrapper with the position-injected atom which was not derefed there, however
  the dev-only validation caused it to be derefed, modifying its render behaviour and causing mayhem and madness for the developer.
  See below that different Reagent types have different ways of retrieving the value without causing capture, although in the case of
  Track, we just deref it as there is no peek or state, so hopefully this won't cause issues (surely this is used very rarely).
  "
  [val-or-atom]
  (if (satisfies? IDeref val-or-atom)
    (cond
      (instance? RAtom    val-or-atom) val-or-atom.state
      (instance? Reaction val-or-atom) (._peek-at val-or-atom)
      (instance? RCursor  val-or-atom) (._peek val-or-atom)
      (instance? Track    val-or-atom) @val-or-atom
      (instance? Wrapper  val-or-atom) val-or-atom.state
      :else                            (throw (js/Error. "Unknown reactive data type")))
    val-or-atom))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; what a sub should return
;; or some simple content ie a string
{:content "abcd"
 :+classes #{:c1 :c2}
 :-classes #{:c3}
 :attrs {:type "text"}}


;;----------------------------------------------------------------------
;; Message
;;----------------------------------------------------------------------
(defn message
  "Displays the message if any otherwise shows nothing. Useful for error messages etc."
  ([attrs opts]
   (message :span attrs opts))
  ([tag attrs opts]
   (let [sub   (-> opts :sub u/as-vector rf/subscribe)]
     (fn [tag attrs _]
       (let [data @sub
             {:keys [content] :as data} (if (map? data)
                                          data
                                          {:content data})
             attrs (-> attrs
                       (merge (:attrs data))
                       (assoc-class data))]
         (when (some? content)
           [tag attrs content]))))))


;;----------------------------------------------------------------------
;; Static Label
;;----------------------------------------------------------------------
(defn label
  "Subscriptions return a map of :content, :=classes, :+classes :-classes.
  :=classes :+classes and :-classes are collections/sets "
  ([attrs opts]
   (label :span attrs opts))
  ([tag attrs opts]
   (let [sub   (-> opts :sub u/as-vector rf/subscribe)]
     (fn [tag attrs _]
       (let [data @sub
             {:keys [content] :as data} (if (map? data)
                                          data
                                          {:content data})
             attrs (-> attrs
                       (merge (:attrs data))
                       (assoc-class data))]
         [tag (assoc-class attrs data) content])))))


;;----------------------------------------------------------------------
;; Text
;;----------------------------------------------------------------------
(defn- text*
  "Subscriptions return a map of :value, :=classes, :+classes :-classes.
  :=classes :+classes and :-classes are collections/sets "
  [attrs opts local-ratom]
  (let [sub            (-> opts :sub u/as-vector rf/subscribe)
        external-value (atom "")]
    (fn [attrs _ _]
      ;; sub returned different value so update external-value
      ;; re-render when sub or local-ratom changes
      ;; - sub when data flows in
      ;; - local-ratom when text is being input
      (when (not= @sub @external-value)
        (reset! external-value @sub)
        (reset! local-ratom @external-value))

      (let [data  @local-ratom
            data  (if (map? data)
                    data
                    {:value data})
            attrs (-> attrs
                      (merge (:attrs data))
                      (assoc :value (:value data))
                      (assoc-class data))]
        [:input attrs]))))

(defn text
  "Subscriptions return a map of :value, :=classes, :+classes :-classes.
  :=classes :+classes and :-classes are collections/sets "
  [attrs opts]
  (let [local-ratom                  (reagent/atom "")
        ;; on-change is either a keyword or vector
        {:keys [on-change on-blur change-on-blur?]
         :or   {change-on-blur? true}} opts
        on-change-fn                 (fn [e]
                                       (let [v (dom/event-value e)]
                                         (reset! local-ratom v)
                                         ;; dispatch if not on blur
                                         (when-not change-on-blur?
                                           (dispatch-event on-change v))))
        on-blur-fn                   (fn [e]
                                       (when change-on-blur?
                                         (dispatch-event on-change @local-ratom))
                                       (when on-blur
                                         (dispatch-event on-blur @local-ratom)))
        attrs                        (merge {:type "text"}
                                            attrs
                                            {:on-change on-change-fn
                                             :on-blur on-blur-fn})]
    [text* attrs opts local-ratom]))


;;----------------------------------------------------------------------
;; Checkbox
;;----------------------------------------------------------------------
(defn- checkbox*
  [attrs opts]
  (let [sub (-> opts :sub u/as-vector rf/subscribe)]
    (fn [attrs _]
      (let [data @sub
            data (if (map? data)
                   data
                   {:checked data})]
        [:input (assoc attrs :checked (true? (:checked data)))]))))

(defn checkbox
  [attrs opts]
  (let [on-change-event (:on-change opts)
        on-change-fn (event-handler on-change-event dom/event-checked)
        attrs (merge attrs
                     {:type "checkbox"
                      :on-change on-change-fn})]
    [checkbox* attrs opts]))



;;----------------------------------------------------------------------
;; Button Controls
;;----------------------------------------------------------------------
(defn- button*
  "assuming content is a string."
  [attrs content opts]
  (let [sub (or (some-> opts :sub u/as-vector rf/subscribe)
                (atom false))
        busy-class (or (:busy-class opts) "button--busy")]
    (fn [attrs content _]
      (let [data @sub
            data (if (map? data)
                   data
                   {:busy? data})
            attrs (if (:busy? data)
                    (-> attrs
                        (assoc :disabled true)
                        (assoc-class (merge {:+classes #{busy-class}} data)))
                    (-> attrs
                        (dissoc :disabled)
                        (assoc-class (merge {:-classes #{busy-class}} data))))]
        ;(log/infof "new-attrs: %s, content: %s, busy: %s" new-attrs content @busy?)
        [:button attrs content]))))

(defn button
  [attrs content opts]
  (let [on-click-fn (-> opts :on-click event-handler)
        attrs (merge attrs {:on-click on-click-fn})]
    [button* attrs content opts]))
