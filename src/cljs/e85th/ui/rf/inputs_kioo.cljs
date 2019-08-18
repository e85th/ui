(ns e85th.ui.rf.inputs-kioo
  (:refer-clojure :exclude [select])
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log]
            ;; [e85th.ui.rf.multi-select :as ms]
            ;; [e85th.ui.rf.paginator :as paginator]
            [e85th.ui.dom :as dom]
            [e85th.ui.time :as time]
            [e85th.ui.moment :as moment]
            [e85th.ui.util :as u]
            [goog.events :as events]
            [clojure.string :as str]
            [clojure.set :as set])
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]
           [goog.date Date DateTime]))

;; (defn dispatch-event
;;   [rf-event event-value & args]
;;   (rf/dispatch (into (u/as-vector rf-event) (cons event-value args))))

;; (defn new-on-change-handler
;;   [rf-event event-reader-fn]
;;   (fn [e]
;;     (dispatch-event rf-event (event-reader-fn e))))

;; (defn rename-class-attr
;;   "attrs is a map of attrs from kioo"
;;   [{:keys [className] :as attrs}]
;;   (-> attrs
;;       (assoc :class className)
;;       (dissoc :className)))

;; (defn compute-class
;;   "Computes the value of the class attr based on existing value
;;    and classes being added and removed"
;;   ([css-class classes-to-add]
;;    (compute-class css-class classes-to-add nil))
;;   ([css-class classes-to-add classes-to-rm]
;;    (let [existing-classes (-> (or css-class "")
;;                               (str/split #" ")
;;                               set)]
;;      (->> (set/difference existing-classes (set classes-to-rm))
;;           (set/union (set classes-to-add))
;;           (str/join " ")))))

;; (defn handle-classes
;;   [{:keys [add-classes remove-classes] :as attrs}]
;;   (cond-> attrs
;;     (or add-classes remove-classes)
;;     (-> (update :class compute-class add-classes remove-classes)
;;         (dissoc :add-classes :remove-classes))))


;; (defn- ensure-simple-content
;;   [node]
;;   (let [content (or (first (:content node [""]))
;;                     "")]
;;     (when-not (string? content)
;;       (throw (ex-info "Can only deal with simple string content." node)))
;;     content))

;; ;;----------------------------------------------------------------------
;; ;; Label
;; ;;----------------------------------------------------------------------
;; (defn- label-view*
;;   [tag attrs sub]
;;   (let [content (rf/subscribe (u/as-vector sub))]
;;     (fn [tag attrs _]
;;       [tag attrs @content])))

;; (defn label
;;   [sub]
;;   (fn [{:keys [tag attrs] :as node}]
;;     (ensure-simple-content node)
;;     [label-view* tag (rename-class-attr attrs) sub]))


;; ;;----------------------------------------------------------------------
;; ;; Message / Error Message
;; ;;----------------------------------------------------------------------
;; (defn- message-view*
;;   [tag attrs sub]
;;   (let [sub-val (rf/subscribe (u/as-vector sub))]
;;     (fn [tag attrs _]
;;       (let [content @sub-val]
;;         (when (some? content)
;;           [tag attrs content])))))

;; (defn message
;;   "Displays the message if any otherwise shows nothing. Useful for error messages etc."
;;   [sub]
;;   (fn [{:keys [tag attrs] :as node}]
;;     (ensure-simple-content node)
;;     [message-view* tag (rename-class-attr attrs) sub]))

;; ;;----------------------------------------------------------------------
;; ;; Text Controls
;; ;;----------------------------------------------------------------------
;; ;; might need an optional callback fn to modify class add/remove/set
;; (defn- normalize-text-sub
;;   "Returns a map of the attrs. Which facilitates adding/removing classes and potentially
;;    other attrs that don't need processing."
;;   [attrs v]
;;   (cond
;;     (nil? v) (assoc attrs :value "")
;;     (string? v) (assoc attrs :value v)
;;     (map? v) (-> (merge attrs v)
;;                  (assoc :value (:value v ""))
;;                  handle-classes)
;;     :else (throw (ex-info "Unknown type." {:v v :type (type v)}))))

;; (defn- text-view*
;;   [attrs sub event]
;;   (let [sub-val (rf/subscribe (u/as-vector sub))]
;;     (fn [attrs _ _]
;;       [:input (normalize-text-sub attrs @sub-val)])))

;; (defn text
;;   "This is a kioo style function, it is tailored after (k/substitute ...).
;;    This returns a function which returns the same view for react so that
;;    the caller of this function is not re-rendered. Only the text field
;;    will be re-rendered. The sub can yield either a string or a map. If
;;    a map, then it should have at least the key `:value` and can have
;;    `:add-classes` and `:remove-classes`"
;;   [sub event]
;;   (fn [{:keys [attrs] :as node}]
;;     ;; return the same view, sub in other fn limits what's re-rendered
;;     (let [attrs (-> (rename-class-attr attrs)
;;                     (assoc :on-change (new-on-change-handler event dom/event-value)))]
;;       [text-view* attrs sub event])))


;; ;;----------------------------------------------------------------------
;; ;; Checkbox Controls
;; ;;----------------------------------------------------------------------
;; (defn- checkbox-view*
;;   [tag attrs sub event]
;;   (let [checked? (rf/subscribe (u/as-vector sub))
;;         on-change-fn (new-on-change-handler event dom/event-checked)]
;;     (fn [tag attrs _ _]
;;       (let [new-attrs (-> attrs
;;                           rename-class-attr
;;                           (assoc :checked (if (true? @checked?) true false)
;;                                  :on-change on-change-fn))]
;;         [tag new-attrs]))))

;; (defn checkbox
;;   [sub event]
;;   (fn [{:keys [tag attrs] :as node}]
;;     [checkbox-view* tag (rename-class-attr attrs) sub event]))


;; ;;----------------------------------------------------------------------
;; ;; Button Controls
;; ;;----------------------------------------------------------------------
;; (defn- button-view*
;;   "assuming content is a string."
;;   [tag attrs content busy-sub opts]
;;   (let [busy? (if busy-sub
;;                  (rf/subscribe (u/as-vector busy-sub))
;;                  (atom false))]
;;     (fn [tag attrs content _ _]
;;       (let [new-attrs (if @busy?
;;                         (assoc attrs :disabled true)
;;                         (dissoc attrs :disabled))
;;             class-name (cond-> (:class new-attrs)
;;                          @busy? (compute-class #{"button--busy"}))
;;             new-attrs (assoc new-attrs :class class-name)]
;;         ;(log/infof "new-attrs: %s, content: %s, busy: %s" new-attrs content @busy?)
;;         [tag new-attrs content]))))

;; (defn button
;;   "kioo style function tailored after (k/substitute ...)"
;;   ([event]
;;    (button nil event))
;;   ([busy-sub event]
;;    (button busy-sub event {}))
;;   ([busy-sub event opts]
;;    (fn [{:keys [tag attrs] :as node}]
;;      (let [button-content (or (:content opts) (ensure-simple-content node))
;;            event (u/as-vector event)
;;            attrs (-> (rename-class-attr attrs)
;;                      (assoc :on-click #(rf/dispatch event)))]
;;        [button-view* tag attrs button-content busy-sub opts]))))


;; ;;----------------------------------------------------------------------
;; ;; Select Controls
;; ;;----------------------------------------------------------------------
;; (defn- select-view*
;;   [tag attrs options-sub selected-sub {:keys [description] :or {description "Select"} :as opts}]
;;   (let [selected (rf/subscribe (u/as-vector selected-sub))
;;         options (rf/subscribe (u/as-vector options-sub))]
;;     (fn [tag attrs _ _]
;;       (let [options-tags (map (fn [{:keys [id name]}]
;;                                 [:option {:key id :value id} name])
;;                               @options)
;;             options-tags (conj options-tags [:option {:key -1 :value -1 :disabled true} description])
;;             select-val @selected
;;             select-value (if (nil? select-val) -1 select-val)]
;;         [tag (assoc attrs :value select-value) options-tags]))))

;; (defn select
;;   ([options-sub selected-sub event]
;;    (select options-sub selected-sub event {}))
;;   ([options-sub selected-sub event opts]
;;    (fn [{:keys [tag attrs] :as node}]
;;      (let [attrs (-> (rename-class-attr attrs)
;;                      (assoc :on-change (new-on-change-handler event dom/event-value)))]
;;        [select-view* tag attrs options-sub selected-sub opts]))))


;; (defn- on-file-selected
;;   [js-event rf-event]
;;   (when-let [file (dom/event-target-file js-event)]
;;     (rf/dispatch (conj rf-event file)))
;;   ;; need to clear the selected value, so that it's clickable again
;;   ;(set! (.-value element) "")
;;   )

;; ;;----------------------------------------------------------------------
;; ;; File Input
;; ;;----------------------------------------------------------------------
;; ;; https://coderwall.com/p/uer3ow/total-input-type-file-style-control-with-pure-css
;; (defn file-view*
;;   [tag attrs busy-sub opts]
;;   (let [busy? (if busy-sub
;;                 (rf/subscribe (u/as-vector busy-sub))
;;                 (atom false))]
;;     (fn [_ _ _ _]
;;       [tag attrs]
;;       (let [new-attrs (if @busy?
;;                         (assoc attrs :disabled true)
;;                         (dissoc attrs :disabled))
;;             class-name (cond-> (:class new-attrs)
;;                          @busy? (compute-class #{"file-input--busy"}))
;;             new-attrs (assoc new-attrs :class class-name)]
;;         ;(log/infof "new-attrs: %s, busy?: %s" new-attrs @busy?)
;;         [tag new-attrs]))))


;; (defn file
;;   ([event]
;;    (file nil event))
;;   ([sub event]
;;    (file sub event {}))
;;   ([sub event opts]
;;    (fn [{:keys [tag attrs] :as node}]
;;      (let [event (u/as-vector event)
;;            attrs (-> (rename-class-attr attrs)
;;                      (assoc :on-change (fn [e]
;;                                          (on-file-selected e event))))]
;;        [file-view* tag attrs sub opts]))))

;; ;;-- Date Picker
;; (defn date-picker-cb
;;   "Date picker with callback for composing components in certain cases."
;;   ([date-value on-change]
;;    [date-picker-cb "Date" date-value on-change])
;;   ([placeholder date-value on-change]
;;    (let [dom-id (str (gensym "date-picker-"))
;;          date-picker (atom nil)]
;;      (reagent/create-class
;;       {:display-name "date-picker"
;;        :reagent-render (fn [placeholder date-value on-change]
;;                          (when @date-picker
;;                            (.setDate @date-picker date-value))
;;                          [:input {:id dom-id :placeholder placeholder}])
;;        :component-did-mount (fn []
;;                               (let [element (goog.dom.getElement dom-id)]
;;                                 (.decorate @date-picker element)
;;                                 (.setShowWeekNum (.getDatePicker @date-picker) false)
;;                                 (some->> date-value (.setDate @date-picker))))
;;        :component-will-mount (fn []
;;                                (let [date-fmt-str "MM/dd/yyyy"
;;                                      fmt (DateTimeFormat. date-fmt-str)
;;                                      parser (DateTimeParse. date-fmt-str)]
;;                                  (reset! date-picker (InputDatePicker. fmt parser))
;;                                  (events/listen @date-picker goog.ui.DatePicker.Events.CHANGE #(on-change (some-> % .-date)))))
;;        :component-will-unmount (fn []
;;                                  (some-> @date-picker .dispose)
;;                                  (reset! date-picker nil))}))))
;; (defn date-picker
;;   "Note that the datepicker dispatches a goog.date.Date instance."
;;   ([sub event]
;;    [date-picker sub event "Date"])
;;   ([sub event placeholder]
;;    (let [date-value (rf/subscribe (u/as-vector sub))]
;;      (fn [sub event placeholder]
;;        [date-picker-cb placeholder @date-value #(dispatch-event event %)]))))

;; (defn moment-date-picker
;;   "Similar to datepicker but converts to from moment."
;;   ([sub event]
;;    [moment-date-picker sub event "Date"])
;;   ([sub event placeholder]
;;    (let [dt-or-m (rf/subscribe (u/as-vector sub))]
;;      (fn [sub event placeholder]
;;        (let [date-value (some-> @dt-or-m moment/coerce moment/goog-date)]
;;          [date-picker-cb placeholder date-value #(dispatch-event event (moment/coerce %))])))))

;; (defn time-picker-cb
;;   "The on-time-selected is invoked with a tuple [hr min time]
;;    hr is an integer from 0-23 and min is an integer from 0-59, time is an integer representing hr + time ie hr = 16, min = 30
;;    then time is 1630 as an int. The input selected-time is an integer."
;;   ([time-value on-change]
;;    [time-picker-cb time-value on-change {:placeholder "Time"}])
;;   ([time-value on-change props]
;;    (let [div-id (str (gensym "time-picker-"))
;;          $tp #(js/$ (str "#" div-id))
;;          ;; only use the hour and minutes, rest is bogus
;;          read-time #(when-let [dt (.timepicker ($tp) "getTime")]
;;                       (let [h (.getHours dt)
;;                             m (.getMinutes dt)]
;;                         [h m (time/hr+min h m)]))
;;          time->date (fn [t]
;;                       ;(log/infof "control get time: %s" t)
;;                       (let [[h m] (time/time->hr+min t)]
;;                         (doto (js/Date.)
;;                           (.setHours h)
;;                           (.setMinutes m))))]
;;      (reagent/create-class
;;       {:display-name "time-picker"
;;        :reagent-render (fn [time-value on-change props]
;;                          ;(log/infof "render time-value: %s" time-value)
;;                          (some-> ($tp) (.timepicker "setTime" (some-> time-value time->date)))
;;                          [:input (assoc props :id div-id :on-blur (comp on-change read-time))])
;;        :component-did-mount (fn []
;;                               (doto ($tp)
;;                                 (.timepicker)
;;                                 (.timepicker "setTime" (some-> time-value time->date))
;;                                 (.on "timeFormatError" #(on-change nil))
;;                                 (.on "timeRangeError" #(on-change nil))
;;                                 (.on "changeTime" (comp on-change read-time))))
;;        :component-will-unmount (fn []
;;                                  (some-> ($tp) (.timepicker "remove")))}))))
;; (defn time-picker
;;   ([sub event]
;;    [time-picker sub event {:placeholder "Time"}])
;;   ([sub event props]
;;    (let [time-value (rf/subscribe (u/as-vector sub))]
;;      (fn [sub event props]
;;        [time-picker-cb @time-value #(dispatch-event event %) props]))))


;; (defn moment-date-time-picker
;;   ""
;;   ([timezone-sub date-sub event]
;;    (let [timezone (rf/subscribe (u/as-vector timezone-sub))
;;          dt (rf/subscribe (u/as-vector date-sub))
;;          state (atom {})
;;          combine-and-dispatch (fn [{:keys [year month date hour minute tz] :as current-state}]
;;                                 ;(log/infof "current-state: %s " current-state)
;;                                 (when (and year month date hour minute tz)
;;                                   (let [dt-str (moment/str-no-tz year month date hour minute 0 0)
;;                                         new-moment (moment/tz dt-str tz)]
;;                                     ;(log/infof "dt-str: %s, tz: %s, new-moment: %s" dt-str tz new-moment)
;;                                     (when (moment/valid? new-moment)
;;                                       (dispatch-event event new-moment)))))
;;          state-update-date (fn [goog-date]
;;                              (let [[y m d] (some-> goog-date time/deconstruct)
;;                                    data {:year y :month m :date d}
;;                                    existing-data (select-keys @state [:year :month :date])]
;;                                ;(log/infof "state-update-date existing-data: %s data: %s" existing-data data)
;;                                (when (not= data existing-data)
;;                                  (swap! state merge data))))
;;          state-update-time (fn [[hr minute]]
;;                              (let [existing-data (select-keys @state [:hour :minute])
;;                                    data {:hour hr :minute minute}]
;;                                ;(log/infof "state-update-time existing-data: %s data: %s" existing-data data)
;;                                (when (not= data existing-data)
;;                                  (swap! state merge data))))
;;          on-date-changed (fn [goog-date]
;;                            (when (state-update-date goog-date)
;;                              (combine-and-dispatch @state)))
;;          on-time-changed(fn [time-data]
;;                           (when (state-update-time time-data)
;;                             (combine-and-dispatch @state)))]
;;      (fn [timezone-sub date-sub event]
;;        (let [m (some-> @dt moment/coerce)
;;              tz @timezone
;;              m-adj (when (and m tz) (moment/tz m tz))
;;              goog-date (some-> m-adj moment/goog-date)
;;              hour (some-> m moment/hour)
;;              minute (some-> m moment/minute)
;;              time-val (when (and hour minute)
;;                         (moment/hr+min hour minute))]

;;          ;(log/infof "hour: %s, minute: %s, time-val: %s" hour minute time-val)
;;          (swap! state assoc :tz tz)
;;          (some-> goog-date state-update-date)
;;          (when time-val
;;            (state-update-time [hour minute time-val]))

;;          [:span
;;           [date-picker-cb goog-date on-date-changed]
;;           [time-picker-cb time-val on-time-changed]])))))




;; ;; (def multi-select ms/multi-select)

;; ;; (def paginator paginator/paginator)


;; (defn tag-editor
;;   ([tag-sub tag-added-event tag-removed-event]
;;    (tag-editor tag-sub
;;     {:onTagAdd (fn [event tag]
;;                  (dispatch-event tag-added-event tag))
;;      :onTagRemove (fn [event tag]
;;                     (dispatch-event tag-removed-event tag))}))
;;   ([tag-sub opts]
;;    (let [element-id (str (gensym "tag-editor-"))
;;          tags (rf/subscribe (u/as-vector tag-sub))
;;          taggle (atom nil)]
;;      (reagent/create-class
;;       {:display-name "tag-editor"
;;        :reagent-render (fn []
;;                          (log/infof "subscribed tags: %s" @tags)
;;                          (when @taggle
;;                            (.setOptions @taggle (clj->js (merge opts {:tags @tags}))))
;;                          [:div {:id element-id :class "tag-container"}])
;;        :component-did-mount (fn []
;;                               (reset! taggle (js/Taggle. element-id
;;                                                          (clj->js (merge opts {:tags (or @tags [])})))))}))))

;; ;; (defn tag-editor-suggester
;; ;;   "NB. tag-added-event fires twice when suggestion is made. Will have to fix. "
;; ;;   [tags-sub suggestions-sub text-changed-event tag-added-event tag-removed-event format-fn]
;; ;;   (let [element-id (str (gensym "tag-editor-"))
;; ;;         awesomplete (atom nil)
;; ;;         taggle-input-sel (str "#" element-id " .taggle_input")
;; ;;         suggestions (rf/subscribe (u/as-vector suggestions-sub))
;; ;;         display->item (atom {})
;; ;;         taggle (atom nil)
;; ;;         tags (rf/subscribe (u/as-vector tags-sub))
;; ;;         opts {:onTagAdd (fn [e tag]
;; ;;                           (dispatch-event tag-added-event tag {:suggestion? false}))
;; ;;               :onTagRemove (fn [event tag]
;; ;;                              (dispatch-event tag-removed-event tag))}]

;; ;;     (reagent/create-class
;; ;;      {:display-name "tag-editor"
;; ;;       :reagent-render (fn [_ _ _ _ _ _]
;; ;;                         (let [suggested-items @suggestions]
;; ;;                           ;(log/infof "suggested-items: %s, display->item: %s" suggested-items @display->item)
;; ;;                           ;(log/infof "awesomplete is: %s" @awesomplete)
;; ;;                           (when (and @awesomplete suggested-items)
;; ;;                             (reset! display->item (reduce (fn [ans x]
;; ;;                                                             (assoc ans (format-fn x) x))
;; ;;                                                           {}
;; ;;                                                           suggested-items))
;; ;;                             (log/infof "display-item keys: %s" (keys @display->item))
;; ;;                             (set! (.-list @awesomplete) (clj->js (or (keys @display->item)
;; ;;                                                                      []))))

;; ;;                           ;(log/infof "tags: %s" @tags)
;; ;;                           (when @taggle
;; ;;                             ;; NB. tried doing setOptions but that doesn't seem to update the tags in the view.
;; ;;                             (.add @taggle (clj->js (or @tags []))))
;; ;;                           [:div {:id element-id :class "tag-container"}]))
;; ;;       :component-did-mount (fn []
;; ;;                              (reset! taggle (js/Taggle. element-id (clj->js (assoc opts :tags (or @tags [])))))
;; ;;                              (-> js/document
;; ;;                                  (.querySelector taggle-input-sel)
;; ;;                                  (.addEventListener "keypress" (fn [e]
;; ;;                                                                  (let [key-value (dom/key-event-value e)
;; ;;                                                                        key-target-value (dom/event-target-value e)
;; ;;                                                                        new-value (str key-target-value key-value)]
;; ;;                                                                    (dispatch-event text-changed-event new-value)))))
;; ;;                              (init-awesomplete taggle-input-sel
;; ;;                                                 awesomplete
;; ;;                                                 display->item
;; ;;                                                 nil
;; ;;                                                 (fn [x]
;; ;;                                                   (dispatch-event tag-added-event x {:suggestion? true}))))})))


;; (defn if-view
;;   ([cond-sub true-view]
;;    (if-view cond-sub true-view [:span]))
;;   ([cond-sub true-view false-view]
;;    (let [cond-val (rf/subscribe (u/as-vector cond-sub))]
;;      (fn [_ _ _]
;;        (if (true? @cond-val)
;;          true-view
;;          false-view)))))
