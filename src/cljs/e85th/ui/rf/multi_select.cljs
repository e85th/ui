(ns e85th.ui.rf.multi-select
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.dom :as dom]
            [e85th.ui.util :as u]
            [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet]]))

(defn selected-values
  [dom-id rf-event]
  (rf/dispatch (conj (u/as-vector rf-event) (dom/selected-option-values dom-id))))

(defsnippet option "templates/e85th/ui/rf/multi-select.html" [:.available :select [:option first-child]]
  [{:keys [id name]}]
  {[:option] (k/do->
              (k/set-attr :key id :value id)
              (k/content name))})

(defsnippet multi-select* "templates/e85th/ui/rf/multi-select.html" [:.multi-select-control]
  [available-opts selected-opts selection-cb deselection-cb avail-dom-id sel-dom-id opts]
  {[:.available :select] (k/do->
                          (k/set-attr :id avail-dom-id)
                          (k/content (map option available-opts)))
   [:.select-btn] (k/listen :on-click #(selection-cb (dom/selected-option-values avail-dom-id)))
   [:.deselect-btn] (k/listen :on-click #(deselection-cb (dom/selected-option-values sel-dom-id)))
   [:.available-title] (k/content (:available-title opts))
   [:.selected-title] (k/content (:selected-title opts))
   [:.selected :select] (k/do->
                         (k/set-attr :id sel-dom-id)
                         (k/content (map option selected-opts)))})

(defn multi-select-cb
  "avail-opts and selected-opts should be seq of maps with keys :id :name.
   cb are callbacks that receive a vector of string keys of the currently selected option values."
  [available-opts selected-opts selection-cb deselection-cb opts]
  [multi-select* available-opts selected-opts selection-cb deselection-cb (str (gensym "multi-select-avail-")) (str (gensym "multi-select-selected-")) opts])

(defn multi-select
  [available-opts-sub selected-opts-sub selection-event deselection-event opts]
  (let [avail-opts (rf/subscribe (u/as-vector available-opts-sub))
        sel-opts (rf/subscribe (u/as-vector selected-opts-sub))
        avail-dom-id (str (gensym "multi-select-avail-"))
        desel-dom-id (str (gensym "multi-select-selected-"))
        selection-event (u/as-vector selection-event)
        deslection-event (u/as-vector deselection-event)]
    (fn [_ _ _ _ _]
      [multi-select* @avail-opts @sel-opts #(rf/dispatch (conj selection-event %)) #(rf/dispatch (conj deselection-event %)) avail-dom-id desel-dom-id opts])))

(defcard-rg multi-select
  [multi-select*
   [{:id 1 :name "A"} {:id 2 :name "B"} {:id 3 :name "C"} {:id 4 :name "D"}]
   [{:id 5 :name "E"} {:id 6 :name "F"}]
   (fn [xs] (log/infof "selected: %s" xs))
   (fn [xs] (log/infof "de-selected: %s" xs))
   "avail-sel"
   "desel-sel"
   {:available-title "Available"
    :selected-title "Selected"}])
