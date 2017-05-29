(ns e85th.ui.rf.inputs
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [devcards.core :as d :refer-macros [defcard defcard-rg]]
            [schema.core :as s]
            [e85th.ui.places :as places]
            [e85th.ui.rf.multi-select :as ms]
            [e85th.ui.rf.paginator :as paginator]
            [e85th.ui.dom :as dom]
            [e85th.ui.time :as time]
            [e85th.ui.moment :as moment]
            [e85th.ui.util :as u]
            [goog.events :as events])
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]
           [goog.date Date DateTime]
           [goog.ui.ac Remote]
           [goog.fx DragListGroup DragListDirection]))

(def input-html "templates/e85th/ui/rf/inputs.html")

(defn dispatch-event
  [rf-event event-value & args]
  (rf/dispatch (into (u/as-vector rf-event) (cons event-value args))))

(defn new-on-change-handler
  [rf-event event-reader-fn]
  (fn [e]
    (dispatch-event rf-event (event-reader-fn e))))

(defn set-attrs-and-events
  [attrs-map events-map]
  ;; seq on a map results in [[k1 v1] [k2 v2]]
  (k/do->
   (apply k/set-attr (flatten (seq attrs-map)))
   (apply k/listen (flatten (seq events-map)))))

(defsnippet error-block* "templates/e85th/ui/rf/inputs.html" [:span.error-block :> any-node]
  [error]
  {[:.error-message] (k/content error)})

(defn error-block
  [error]
  (if error
    (error-block* error)
    ""))

(defsnippet standard-text-input* "templates/e85th/ui/rf/inputs.html" [:div.standard-text-input]
  [attrs-map events-map error]
  {[:input] (k/do->
             (set-attrs-and-events attrs-map events-map)
             (k/after (error-block error)))})

(defsnippet standard-password-input* "templates/e85th/ui/rf/inputs.html" [:div.standard-password-input]
  [attrs-map events-map error]
  {[:input] (k/do->
             (set-attrs-and-events attrs-map events-map)
             (k/after (error-block error)))})


(defn rf-text-input
  "re-framed text-input, subscribes and updates, dispatches on-change event
  error-sub-or-fn can be either a keyword, vector (for re-frame subscription),
  nil or a function. If it's a function, it takes the current text value and
  returns nil if no validation errors or a string indicating the error message.
  The view is a function that takes attrs-map, events-map and error-message."
  [view error-sub-or-fn sub event attrs-map events-map]
  (let [text (rf/subscribe (u/as-vector sub))
        error (cond
                (nil? error-sub-or-fn) (atom nil)
                (fn? error-sub-or-fn) nil
                (vector? error-sub-or-fn) (rf/subscribe error-sub-or-fn)
                (keyword? error-sub-or-fn) (rf/subscribe [error-sub-or-fn])
                :else (throw (js/Error. (str "Don't know how to deal with type: " (type error-sub-or-fn)))))]
    (fn [view error-sub-or-fn sub event attrs-map events-map]
      ;; error will be nil only if error-sub-or-fn was a function during setup
      (let [text-error (if (fn? error-sub-or-fn)
                         (error-sub-or-fn @text)
                         @error)]
        [view
         (assoc attrs-map :value (or @text ""))
         (assoc events-map :on-change (new-on-change-handler event dom/event-value))
         text-error]))))

(defn new-text-input
  ([view error-sub-or-fn sub event]
   (new-text-input view error-sub-or-fn sub event {}))
  ([view error-sub-or-fn sub event attrs-map]
   (new-text-input view error-sub-or-fn sub event attrs-map {}))
  ([view error-sub-or-fn sub event attrs-map events-map]
   [rf-text-input view error-sub-or-fn sub event attrs-map events-map]))


(def ^{:doc "Text input field without visual cues for error validation."}
  std-text (partial new-text-input standard-text-input* nil))

(def ^{:doc "Text input field with validaiton and visual cues."}
  text (partial new-text-input standard-text-input*))

(def ^{:doc "Password input field without visual cues for error validation."}
  std-password (partial new-text-input standard-password-input* nil))

(def ^{:doc "Password input field with validaiton and visual cues."}
  password (partial new-text-input standard-password-input*))

(defsnippet url* "templates/e85th/ui/rf/inputs.html" [:div.url-input]
  [attrs-map events-map]
  {[:input] (set-attrs-and-events attrs-map events-map)})

(def ^{:doc "URL input field without visual validation except for what the browser supports"}
  std-url (partial new-text-input url* nil))

(def ^{:doc "URL input field with visual validation."}
  url (partial new-text-input url*))

(defsnippet standard-checkbox* "templates/e85th/ui/rf/inputs.html" [:div.standard-checkbox]
  [attrs-map events-map label]
  {[:input] (k/do->
             (set-attrs-and-events attrs-map events-map)
             (if label
               (k/after label)
               identity))})

(defn rf-checkbox
  "re-framed text-input, subscribes and updates, dispatches on-change event"
  [view sub event attrs-map events-map label]
  (let [checked? (rf/subscribe (u/as-vector sub))]
    (fn [view sub event attrs-map events-map]
      ;; @checked? should be a boolean
      [view
       (assoc attrs-map :checked @checked?)
       (assoc events-map :on-change (new-on-change-handler event dom/event-checked))
       label])))


(defn checkbox
  ([sub event]
   (checkbox sub event nil))
  ([sub event label]
   [rf-checkbox standard-checkbox* sub event {} {} label]))


(defsnippet standard-button* "templates/e85th/ui/rf/inputs.html" [:span.standard-button]
  [attrs-map events-map content busy?]
  {[:button] (k/do->
              (set-attrs-and-events attrs-map events-map)
              (k/content content))
   [:.busy-indicator] (if busy?
                        identity
                        (k/substitute ""))})

(defn rf-button
  ([view event content]
   (rf-button view nil event content))
  ([view busy-sub event content]
   (let [busy? (if busy-sub
                 (rf/subscribe (u/as-vector busy-sub))
                 (atom false))
         event-v (u/as-vector event)]
     (fn [view busy-sub event content]
       [view {:disabled @busy?} {:on-click #(rf/dispatch event-v)} content @busy?]))))

(defn button
  ([event content]
   (button nil event content))
  ([sub event content]
    [rf-button standard-button* sub event content]))


(defn rf-label
  [view sub]
  (let [v (rf/subscribe (u/as-vector sub))]
    (fn [view sub]
      [view @v])))

(defn label
  [sub]
  [rf-label :span.form-control-static sub])

(defn rf-error-msg
  [view sub]
  (let [v (rf/subscribe (u/as-vector sub))]
    (fn [view sub]
      (when (seq @v)
        [view @v]))))

(defn error-msg
  [sub]
  [rf-error-msg :div.error-msg sub])


(defn rf-select
  [view selected-sub options-sub attrs-map events-map select-description]
  (let [selected (rf/subscribe (u/as-vector selected-sub))
        options (rf/subscribe (u/as-vector options-sub))]
    (fn [view selected-sub options-sub attrs-map events-map select-description]
      (let [option-tags (map (fn [{:keys [id name]}]
                               [:option {:key id :value id} name])
                             @options)
            option-tags (conj option-tags [:option {:key -1 :value -1 :disabled true} select-description])
            ;; to handle boolean false
            opt-value (if (some? @selected) @selected -1)]
        ;(log/infof "selected: %s, opt-value: %s" @selected opt-value)
        [view
         (merge attrs-map events-map {:value opt-value})
         option-tags]))))

(defn select
  "options-sub should yield a seq of  maps with keys :id and :name."
  ([selected-sub event options-sub]
   (select selected-sub event options-sub "Select"))
  ([selected-sub event options-sub select-description]
   [rf-select :select selected-sub options-sub {} {:on-change #(rf/dispatch (conj (u/as-vector event) (dom/event-value %)))} select-description]))

;;--- Google Places / Address Suggest
(defsnippet places-autocomplete* "templates/e85th/ui/rf/inputs.html" [:div.places-autocomplete]
  [element-id display-address-ratom]
  {[:input] (k/do->
             (k/set-attr :id element-id :value @display-address-ratom)
             (k/listen :on-change #(reset! display-address-ratom (dom/event-value %))))})

(defn places-autocomplete-cb
  ""
  [display-address-ratom on-change]
  (let [element-id (str (gensym "places-autocomplete-"))]
    (reagent/create-class
     {:display-name "places-autocomplete"
      :reagent-render (fn [display-address-ratom on-change]
                        ;(log/infof "places autocomplete rendered")
                        [places-autocomplete* element-id display-address-ratom])
      :component-did-mount (fn []
                             ;(log/infof "places autocomplete mounted")
                             (let [autocomplete (places/new-autocomplete element-id)
                                   handler #(on-change (places/parse-selected-place autocomplete))]
                               (places/add-autocomplete-listener autocomplete handler)))})))

(defn places-autocomplete
  " "
  [sub event]
  (let [v (rf/subscribe (u/as-vector sub))]
    (fn [sub event]
      [places-autocomplete-cb (reagent/atom @v) #(dispatch-event event %)])))

;;-- Date Picker
(defn date-picker-cb
  "Date picker with callback for composing components in certain cases."
  ([date-value on-change]
   [date-picker-cb "Date" date-value on-change])
  ([placeholder date-value on-change]
   (let [dom-id (str (gensym "date-picker-"))
         date-picker (atom nil)]
     (reagent/create-class
      {:display-name "date-picker"
       :reagent-render (fn [placeholder date-value on-change]
                         (when @date-picker
                           (.setDate @date-picker date-value))
                         [:input {:id dom-id :placeholder placeholder}])
       :component-did-mount (fn []
                              (let [element (goog.dom.getElement dom-id)]
                                (.decorate @date-picker element)
                                (.setShowWeekNum (.getDatePicker @date-picker) false)
                                (some->> date-value (.setDate @date-picker))))
       :component-will-mount (fn []
                               (let [date-fmt-str "MM/dd/yyyy"
                                     fmt (DateTimeFormat. date-fmt-str)
                                     parser (DateTimeParse. date-fmt-str)]
                                 (reset! date-picker (InputDatePicker. fmt parser))
                                 (events/listen @date-picker goog.ui.DatePicker.Events.CHANGE #(on-change (some-> % .-date)))))
       :component-will-unmount (fn []
                                 (some-> @date-picker .dispose)
                                 (reset! date-picker nil))}))))
(defn date-picker
  "Note that the datepicker dispatches a goog.date.Date instance."
  ([sub event]
   [date-picker sub event "Date"])
  ([sub event placeholder]
   (let [date-value (rf/subscribe (u/as-vector sub))]
     (fn [sub event placeholder]
       [date-picker-cb placeholder @date-value #(dispatch-event event %)]))))

(defn moment-date-picker
  "Similar to datepicker but converts to from moment."
  ([sub event]
   [moment-date-picker sub event "Date"])
  ([sub event placeholder]
   (let [dt-or-m (rf/subscribe (u/as-vector sub))]
     (fn [sub event placeholder]
       (let [date-value (some-> @dt-or-m moment/coerce moment/goog-date)]
         [date-picker-cb placeholder date-value #(dispatch-event event (moment/coerce %))])))))

(defn time-picker-cb
  "The on-time-selected is invoked with a tuple [hr min time]
   hr is an integer from 0-23 and min is an integer from 0-59, time is an integer representing hr + time ie hr = 16, min = 30
   then time is 1630 as an int. The input selected-time is an integer."
  ([time-value on-change]
   [time-picker-cb time-value on-change {:placeholder "Time"}])
  ([time-value on-change props]
   (let [div-id (str (gensym "time-picker-"))
         $tp #(js/$ (str "#" div-id))
         ;; only use the hour and minutes, rest is bogus
         read-time #(when-let [dt (.timepicker ($tp) "getTime")]
                      (let [h (.getHours dt)
                            m (.getMinutes dt)]
                        [h m (time/hr+min h m)]))
         time->date (fn [t]
                      ;(log/infof "control get time: %s" t)
                      (let [[h m] (time/time->hr+min t)]
                        (doto (js/Date.)
                          (.setHours h)
                          (.setMinutes m))))]
     (reagent/create-class
      {:display-name "time-picker"
       :reagent-render (fn [time-value on-change props]
                         ;(log/infof "render time-value: %s" time-value)
                         (some-> ($tp) (.timepicker "setTime" (some-> time-value time->date)))
                         [:input (assoc props :id div-id :on-blur (comp on-change read-time))])
       :component-did-mount (fn []
                              (doto ($tp)
                                (.timepicker)
                                (.timepicker "setTime" (some-> time-value time->date))
                                (.on "timeFormatError" #(on-change nil))
                                (.on "timeRangeError" #(on-change nil))
                                (.on "changeTime" (comp on-change read-time))))
       :component-will-unmount (fn []
                                 (some-> ($tp) (.timepicker "remove")))}))))
(defn time-picker
  ([sub event]
   [time-picker sub event {:placeholder "Time"}])
  ([sub event props]
   (let [time-value (rf/subscribe (u/as-vector sub))]
     (fn [sub event props]
       [time-picker-cb @time-value #(dispatch-event event %) props]))))


(defn moment-date-time-picker
  ""
  ([timezone-sub date-sub event]
   (let [timezone (rf/subscribe (u/as-vector timezone-sub))
         dt (rf/subscribe (u/as-vector date-sub))
         state (atom {})
         combine-and-dispatch (fn [{:keys [year month date hour minute tz] :as current-state}]
                                ;(log/infof "current-state: %s " current-state)
                                (when (and year month date hour minute tz)
                                  (let [dt-str (moment/str-no-tz year month date hour minute 0 0)
                                        new-moment (moment/tz dt-str tz)]
                                    ;(log/infof "dt-str: %s, tz: %s, new-moment: %s" dt-str tz new-moment)
                                    (when (moment/valid? new-moment)
                                      (dispatch-event event new-moment)))))
         state-update-date (fn [goog-date]
                             (let [[y m d] (some-> goog-date time/deconstruct)
                                   data {:year y :month m :date d}
                                   existing-data (select-keys @state [:year :month :date])]
                               ;(log/infof "state-update-date existing-data: %s data: %s" existing-data data)
                               (when (not= data existing-data)
                                 (swap! state merge data))))
         state-update-time (fn [[hr minute]]
                             (let [existing-data (select-keys @state [:hour :minute])
                                   data {:hour hr :minute minute}]
                               ;(log/infof "state-update-time existing-data: %s data: %s" existing-data data)
                               (when (not= data existing-data)
                                 (swap! state merge data))))
         on-date-changed (fn [goog-date]
                           (when (state-update-date goog-date)
                             (combine-and-dispatch @state)))
         on-time-changed(fn [time-data]
                          (when (state-update-time time-data)
                            (combine-and-dispatch @state)))]
     (fn [timezone-sub date-sub event]
       (let [m (some-> @dt moment/coerce)
             tz @timezone
             m-adj (when (and m tz) (moment/tz m tz))
             goog-date (some-> m-adj moment/goog-date)
             hour (some-> m moment/hour)
             minute (some-> m moment/minute)
             time-val (when (and hour minute)
                        (moment/hr+min hour minute))]

         ;(log/infof "hour: %s, minute: %s, time-val: %s" hour minute time-val)
         (swap! state assoc :tz tz)
         (some-> goog-date state-update-date)
         (when time-val
           (state-update-time [hour minute time-val]))

         [:span
          [date-picker-cb goog-date on-date-changed]
          [time-picker-cb time-val on-time-changed]])))))




(def multi-select ms/multi-select)

(def paginator paginator/paginator)


(defn tag-editor
  ([tag-sub tag-added-event tag-removed-event]
   (tag-editor tag-sub
    {:onTagAdd (fn [event tag]
                 (dispatch-event tag-added-event tag))
     :onTagRemove (fn [event tag]
                    (dispatch-event tag-removed-event tag))}))
  ([tag-sub opts]
   (let [element-id (str (gensym "tag-editor-"))
         tags (rf/subscribe (u/as-vector tag-sub))
         taggle (atom nil)]
     (reagent/create-class
      {:display-name "tag-editor"
       :reagent-render (fn []
                         (log/infof "subscribed tags: %s" @tags)
                         (when @taggle
                           (.setOptions @taggle (clj->js (merge opts {:tags @tags}))))
                         [:div {:id element-id :class "tag-container"}])
       :component-did-mount (fn []
                              (reset! taggle (js/Taggle. element-id
                                                         (clj->js (merge opts {:tags (or @tags [])})))))}))))

(defn init-awesomplete
  ([dom-selector awesomplete-atom display->item-atom selection-event on-select-fn]
   (init-awesomplete dom-selector awesomplete-atom display->item-atom selection-event on-select-fn (constantly nil)))
  ([dom-selector awesomplete-atom display->item-atom selection-event on-select-fn post-select-fn]
   (reset! awesomplete-atom (js/Awesomplete. (.querySelector js/document dom-selector) #js {:minChars 1}))
   (.on (js/$ dom-selector) "awesomplete-selectcomplete" (fn [e]
                                                           (let [selected (dom/event-value e)
                                                                 selection (@display->item-atom selected)]
                                                             (if selection-event
                                                               (dispatch-event selection-event selection)
                                                               (on-select-fn selection))
                                                             (post-select-fn))))))
(defn awesomplete
  "opts can have keys :format-fn a one arity function to format the suggestions.
   It can have a placeholder as well."
  [suggestions-sub text-changed-event selection-event {:keys [format-fn placeholder clear-input-on-select?] :or {format-fn identity
                                                                                                                 placeholder "Search.."
                                                                                                                 clear-input-on-select? false}}]
  (let [dom-id (str (gensym "awesomplete-"))
        dom-sel (str "#" dom-id)
        awesomplete (atom nil)
        suggestions (rf/subscribe (u/as-vector suggestions-sub))
        display->item (atom {})
        display-ratom (reagent/atom "")
        on-change-fn (fn [e]
                       (let [text (dom/event-value e)]
                         (reset! display-ratom text)
                         (dispatch-event text-changed-event text)))]
    (reagent/create-class
     {:display-name "awesomplete"
      :reagent-render (fn [_ _ _ _]
                        (let [suggested-items @suggestions]
                          (when (and @awesomplete suggested-items)
                            (reset! display->item (reduce (fn [ans x]
                                                            (assoc ans (format-fn x) x))
                                                          {}
                                                          suggested-items))
                            (set! (.-list @awesomplete) (clj->js (keys @display->item)))))
                        [:input {:id dom-id
                                 :value @display-ratom
                                 :placeholder placeholder
                                 :on-change on-change-fn}])
      :component-did-mount (fn []
                             (init-awesomplete dom-sel awesomplete display->item selection-event nil (fn []
                                                                                                       (when clear-input-on-select?
                                                                                                         (reset! display-ratom "")))))})))

(defn tag-editor-suggester
  "NB. tag-added-event fires twice when suggestion is made. Will have to fix. "
  [tags-sub suggestions-sub text-changed-event tag-added-event tag-removed-event format-fn]
  (let [element-id (str (gensym "tag-editor-"))
        awesomplete (atom nil)
        taggle-input-sel (str "#" element-id " .taggle_input")
        suggestions (rf/subscribe (u/as-vector suggestions-sub))
        display->item (atom {})
        taggle (atom nil)
        tags (rf/subscribe (u/as-vector tags-sub))
        opts {:onTagAdd (fn [e tag]
                          (dispatch-event tag-added-event tag {:suggestion? false}))
              :onTagRemove (fn [event tag]
                             (dispatch-event tag-removed-event tag))}]

    (reagent/create-class
     {:display-name "tag-editor"
      :reagent-render (fn [_ _ _ _ _ _]
                        (let [suggested-items @suggestions]
                          ;(log/infof "suggested-items: %s, display->item: %s" suggested-items @display->item)
                          ;(log/infof "awesomplete is: %s" @awesomplete)
                          (when (and @awesomplete suggested-items)
                            (reset! display->item (reduce (fn [ans x]
                                                            (assoc ans (format-fn x) x))
                                                          {}
                                                          suggested-items))
                            (log/infof "display-item keys: %s" (keys @display->item))
                            (set! (.-list @awesomplete) (clj->js (or (keys @display->item)
                                                                     []))))

                          ;(log/infof "tags: %s" @tags)
                          (when @taggle
                            ;; NB. tried doing setOptions but that doesn't seem to update the tags in the view.
                            (.add @taggle (clj->js (or @tags []))))
                          [:div {:id element-id :class "tag-container"}]))
      :component-did-mount (fn []
                             (reset! taggle (js/Taggle. element-id (clj->js (assoc opts :tags (or @tags [])))))
                             (-> js/document
                                 (.querySelector taggle-input-sel)
                                 (.addEventListener "keypress" (fn [e]
                                                                 (let [key-value (dom/key-event-value e)
                                                                       key-target-value (dom/event-target-value e)
                                                                       new-value (str key-target-value key-value)]
                                                                   (dispatch-event text-changed-event new-value)))))
                             (init-awesomplete taggle-input-sel
                                                awesomplete
                                                display->item
                                                nil
                                                (fn [x]
                                                  (dispatch-event tag-added-event x {:suggestion? true}))))})))
