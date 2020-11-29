(ns e85th.ui.widgets
  (:require [reagent.core :as reagent]
            [e85th.ui.dom :as dom]
            [e85th.commons.ext :as ext]))

(defn dropdown-0
  "Dropdown with checkboxes.  click anywhere outside the dropdown close it.
  Items selected have a checkbox appear on the left of the item.
   `:on-change` fires with all selected values.
  `:value` is a seq of selected ids.
  `:options` is a seq of maps with keys `:id`, `:text`"
  [{:keys [on-change]}]
  (let [state     (reagent/atom {:open?    false
                                 :dirty?   false
                                 :touched? false
                                 :ids      #{}})
        toggle       (fn [_e]
                       (let [{:keys [open? el ids dirty?]} (swap! state update :open? not)]
                         ;; focus so on blur works later else you have to click the actual element for blur to work
                         (when (and open? el)
                           (js/setTimeout #(.focus el) 0))
                         (when (and dirty? (not open?))
                           (on-change ids)
                           (swap! state assoc :dirty? false))))
        on-ref       (fn [el]
                       (swap! state assoc :el el))
        item-clicked (fn [id _e]
                       (let [{:keys [ids]} @state
                             f             (if (ids id) disj conj)]
                         (swap! state (fn [state]
                                        (-> state
                                            (update :ids f id)
                                            (assoc :dirty? true :touched? true))))))]
    (fn [{:keys [value options placeholder]
         :or {placeholder "Select"}}]
      (let [{:keys [open? ids]}        (if (:touched? @state)
                                         @state
                                         (swap! state assoc :ids (set value)))]

        [:div.dropdown
         ;; (pr-str @state)
         [:div {:tab-index "-1"
                :on-blur   toggle}
          [:span {:on-click toggle} placeholder]
          [:ul.dropdown__list {:class (when open? "open")
                               :ref   on-ref}
           (for [{:keys [id text]} options]
             [:li.dropdown__list-item {:key      id
                                       :on-click (partial item-clicked id)}
              [:div
               [:span {:style {:visibility (if (ids id) "visible" "hidden")}} "✓ "]
               [:span text]]])]]]))))


(defn dropdown
  "Dropdown emulating semantic ui's multi select dropdown. Items selected are removed
  from dropdown and added to selections as chips with delete icons next to each.
   `:on-change` fires with all selected values.
  `:delay` in millis used to control change dispatch when deleting
   selected items defaults to zero, for no save button edits, set to 2000 or something
  `:options` is a seq of maps with keys `:id`, `:text`"
  [{:keys [on-change delay]
    :or   {delay 0} :as _attrs}]
  (let [state             (reagent/atom {:open?         false
                                         :dirty?        false
                                         :touched?      false
                                         :id->options   {}
                                         :selected      #{}
                                         :init-selected #{}})
        dispatch-change!  (fn []
                            (let [{:keys [dirty? selected open? init-selected]} (swap! state identity)]
                              (when (and dirty? (not open?) (not= selected init-selected))
                                (on-change selected)
                                (swap! state assoc :dirty? false :init-selected selected))))
        toggle            (fn [_e]
                            (let [{:keys [open? el dirty?]} (swap! state update :open? not)]
                              ;; focus so on blur works later else you have to click the actual element for blur to work
                              (when (and open? el)
                                (js/setTimeout #(.focus el) 0))
                              (when (and dirty? (not open?))
                                (dispatch-change!))))
        on-blur           (fn [_e]
                            (swap! state assoc :open? false)
                            (dispatch-change!))
        on-ref            (fn [el] (swap! state assoc :el el))
        selection-changed (fn [state fn id]
                            (-> state
                                (update :selected fn id)
                                (assoc :dirty? true :touched? true)))
        item-deleted      (fn [id]
                            (swap! state selection-changed disj id)
                            ;; need delay if blur doesn't happen
                            (js/setTimeout dispatch-change! delay))
        item-clicked      (fn [id e]
                            (dom/event-prevent-defaults e) ; don't let toggle get called
                            (swap! state selection-changed conj id))
        init-state        (fn [state {:keys [value options]}]
                            (assoc state
                                   :selected (set value)
                                   :init-selected (set value)
                                   :id->options (ext/group-by+ :id identity first options)))]
    (fn [{:keys [options placeholder] :as attrs
         :or   {placeholder "Select"}}]
      (let [{:keys [open? selected id->options]} (if (:touched? @state)
                                                   @state
                                                   (swap! state init-state attrs))]
        [:div
         ;; [:div.debug (pr-str @state)]
         [:div.dropdown {:tab-index "-1"
                         :on-blur   on-blur
                         :on-click  toggle}

          [:div.dropdown__selections {}
           (when-not (seq selected)
             [:span placeholder])
           (for [{:keys [id text]} (->> (map id->options selected)
                                        (sort-by :text))]
             [:div.chip.chip--light {:key      (str "sel-" id)
                                     :on-click dom/event-prevent-defaults} text
              [:i.icon.delete {:on-click (partial item-deleted id)}]])]
          [:i.dropdown__icon.arrow.arrow--down]
          [:ul.dropdown__list {:class (when open? "open")
                               :ref   on-ref}
           (for [{:keys [id text]} (remove (comp selected :id) options)]
             [:li {:key      (str "opt-" id)
                   :on-click (partial item-clicked id)}
              [:div text]])]]]))))
