(ns e85th.ui.rf.maps
  (:require [e85th.ui.places :as places]
            [e85th.ui.util :as u]
            [e85th.ui.rf.inputs :as i]
            [e85th.ui.dom :as dom]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log]))


;;----------------------------------------------------------------------
;; Places Autocomplete
;;----------------------------------------------------------------------
(defn- gpv
  [tag attrs display-ratom]
  [tag (assoc attrs :value @display-ratom)])

(defn google-places-view*
  "Uses a reagent ratom itself for changes to the input field otherwise there's
   a perceptible delay when dispatching an event over requestAnimationFrame"
  [tag attrs sub event]
  (let [element-id (or (:id attrs)
                       (str (gensym "places-autocomplete-")))
        sub-val (rf/subscribe (u/as-vector sub))
        ;; display-ratom acts as a common place to reflect subscription value
        ;; and do a quick update to the UI w/o re-frame dispatch
        display-ratom (reagent/atom "")
        on-change (fn [e]
                    ;(log/infof "on-change handler called")
                    (reset! display-ratom (dom/event-value e))) ; update right now, don't give to re-frame
        attrs (assoc attrs :on-change on-change :id element-id)]
    (reagent/create-class
     {:display-name "places-autocomplete"
      :reagent-render
      (fn [_ _ _ _]
        ;; deref sub here so that, this fn gets called on changes
        (reset! display-ratom @sub-val)
        ;; Do *NOT* deref the display-ratom in this function
        ;; if display-ratom is derefed here, then the previous reset runs
        ;; and the UI seems incapable of being edited
        [gpv tag attrs display-ratom])

      :component-did-mount
      (fn []
        (let [autocomplete (places/new-autocomplete element-id) ; need to dispose?
              handler #(i/dispatch-event event (places/parse-selected-place autocomplete))]
          (places/add-autocomplete-listener autocomplete handler)))})))

(defn google-places
  [sub event]
  (fn [{:keys [tag attrs] :as node}]
    ;(log/infof "handle google places inner: %s" attrs)
    (let [attrs (i/rename-class-attr attrs)]
      [google-places-view* tag attrs sub event])))


;;------------------------------------------------------------------------
;; Embedded Google Map
;;------------------------------------------------------------------------

(def ^:const +embed-map-url+ "https://www.google.com/maps/embed/v1/place")

(defn embedded-google-map*
  [tag attrs address-sub opts]
  (let [address (rf/subscribe (u/as-vector address-sub))]
    (fn [_ attrs _]
      (let [q @address
            url (when-not (str/blank? q)
                  (str +embed-map-url+ "?" (u/params->query-string (assoc opts :q q))))]
        [tag (cond-> attrs
               url (assoc :src url))]))))

(defn embedded-google-map
  [address-sub opts]
  (fn [{:keys [tag attrs] :as node}]
    (let [attrs (i/rename-class-attr attrs)]
      [embedded-google-map* tag attrs address-sub opts])))


;;------------------------------------------------------------------------
;; Static Google Map
;;------------------------------------------------------------------------
(def ^:const +static-map-url+ "https://maps.googleapis.com/maps/api/staticmap")

(defn static-google-map*
  [tag attrs address-sub opts]
  (let [address (rf/subscribe (u/as-vector address-sub))]
    (fn [_ attrs _]
      (let [q @address
            url (when-not (str/blank? q)
                  (str +static-map-url+ "?" (u/params->query-string (assoc opts :center q))))]
        [tag (cond-> attrs
               url (assoc :src url))]))))

(defn static-google-map
  [address-sub opts]
  (fn [{:keys [tag attrs] :as node}]
    (let [attrs (i/rename-class-attr attrs)]
      [static-google-map* tag attrs address-sub opts])))
