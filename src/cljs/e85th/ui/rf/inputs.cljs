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
         (assoc events-map :on-change (new-on-change-handler event u/event-value))
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
       (assoc events-map :on-change (new-on-change-handler event u/event-checked))
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
              (k/content content)
              ((if busy? k/add-class k/remove-class) "disabled"))
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
       [view {} {:on-click #(rf/dispatch event-v)} content @busy?]))))

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
   [rf-select :select selected-sub options-sub {} {:on-change #(rf/dispatch (conj (u/as-vector event) (u/event-value %)))} select-description]))

;;--- Google Places / Address Suggest
(defsnippet places-autocomplete* "templates/e85th/ui/rf/inputs.html" [:div.places-autocomplete]
  [element-id display-address-ratom]
  {[:input] (k/do->
             (k/set-attr :id element-id :value @display-address-ratom)
             (k/listen :on-change #(reset! display-address-ratom (u/event-value %))))})

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
  ([sub event]
   [date-picker sub event "Date"])
  ([sub event placeholder]
   (let [date-value (rf/subscribe (u/as-vector sub))]
     (fn [sub event placeholder]
       [date-picker-cb placeholder @date-value #(dispatch-event event %)]))))

;;-- Google Closure Autocomplete
(s/defn autocomplete-cb
  "headers is a map of str->str"
  [url headers display-value-ratom on-select]
  (let [dom-id (str (gensym "auto-complete-"))
        auto-complete (atom nil)]
    (reagent/create-class
     {:display-name "auto-complete"
      :reagent-render (fn [url headers display-value-ratom on-select]
                        [:input {:id dom-id
                                 :value @display-value-ratom
                                 :on-change #(reset! display-value-ratom (u/event-value %))}])
      :component-did-mount (fn []
                             (let [element (goog.dom.getElement dom-id)]
                               (reset! auto-complete (Remote. url element))
                               (-> @auto-complete (.setHeaders (clj->js headers)))
                               (events/listen @auto-complete goog.ui.ac.AutoComplete.EventType.SELECT #(log/infof "autocomplete selecte event"))))
      :component-will-unmount (fn []
                                (some-> @auto-complete .dispose)
                                (reset! auto-complete nil))})))

(defn autocomplete
  [url headers sub event]
  (let [display-value (rf/subscribe (u/as-vector sub))]
    (fn [url headers sub event]
      [autocomplete-cb url headers (reagent/atom @display-value) #(dispatch-event event %)])))


;;--  Typeahead Autocomplete
(s/defn new-bloodhound
  "New Bloodhound tied to an input text field.
   prepare-request-fn takes the query and the settings object and returns a settings object."
  ([remote-url :- s/Str wildcard :- s/Str]
   (new-bloodhound remote-url wildcard (fn [search settings] settings)))
  ([remote-url :- s/Str wildcard :- s/Str prepare-request-fn]
   (let [opts #js {:datumTokenizer (js/Bloodhound.tokenizers.obj.whitespace "value")
                   :queryTokenizer js/Bloodhound.tokenizers.whitespace
                   :remote #js {:url remote-url
                                :wildcard wildcard
                                :prepare prepare-request-fn}}]
     (js/Bloodhound. opts))))

(s/defn init-typeahead!
  "dom id is string that identifies the element that typeahead should hook up to.
   typeahead-options is a map. dataset is a Bloodhound instance.
   suggestion-selected-fn is two arg fn taking an event and the selection,
   called when a selection is chosen."
  [dom-selector typeahead-options dataset suggestion-selected-fn]
  ;(log/infof "init-typeahead with dom-selector: %s" dom-selector)
  (doto (js/$ dom-selector)
    (.typeahead (clj->js typeahead-options) dataset)
    (.bind "typeahead:select" suggestion-selected-fn)))

(defsnippet typeahead* "templates/e85th/ui/rf/inputs.html" [:.search-control]
  [attrs-map events-map]
  {[:.search-control] (set-attrs-and-events attrs-map events-map)})


(defn typeahead-cb
  "typeahead-opts, dataset-opts should be passed in as JS objects not clj maps."
  ([view attrs-map display-value typeahead-opts dataset-opts on-item-selected on-blur]
   (let [dom-id (str (gensym "typeahead-"))
         dom-sel (str "#" dom-id)
         attrs-map (merge {:id dom-id :placeholder "Search"} attrs-map)
         ;set-display (fn [s] (-> dom-sel js/$ (.typeahead "val" s))) ;; this will set the value and trigger a search
         set-display (fn [s] (-> dom-sel js/$ (.data "ttTypeahead") .-input (.setQuery s true))) ;; this sets the value w/o triggering a search
         inited? (atom false)]
     (reagent/create-class
      {:display-name "typeahead"
       :reagent-render (fn [_ _ display-value _ _ _ _]
                         (when @inited?
                           (set-display display-value))
                         [view attrs-map {:on-blur on-blur}])
       :component-did-mount (fn []
                              ;(log/infof "typeahead component mounted")
                              (let [cb (fn [obj datum dataset-name]
                                         (on-item-selected (js->clj datum :keywordize-keys true)))]
                                (init-typeahead! dom-sel typeahead-opts dataset-opts cb)
                                (reset! inited? true)
                                ;; set display after the component is created
                                ;; first time in reagent-render is the inital dom and it does not exist yet
                                (set-display display-value)))}))))

(defn typeahead
  ([selection-event attrs-map typeahead-opts dataset-opts]
   (typeahead nil selection-event attrs-map typeahead-opts dataset-opts))
  ([text-sub selection-event attrs-map typeahead-opts dataset-opts]
   (typeahead text-sub selection-event nil attrs-map typeahead-opts dataset-opts ))

  ([text-sub selection-event blur-event attrs-map typeahead-opts dataset-opts]
   (typeahead typeahead* text-sub selection-event blur-event attrs-map typeahead-opts dataset-opts))

  ([view text-sub selection-event blur-event attrs-map typeahead-opts dataset-opts]

   ;(log/infof "text-sub: %s, sel-ev: %s, blur: %s, attrs: %s, type: %s data: %s"  text-sub selection-event blur-event attrs-map typeahead-opts dataset-opts)

   (let [text (or (some-> text-sub u/as-vector rf/subscribe) (atom ""))]
     (fn [_ _ _ _ _ _ _]
       [typeahead-cb view attrs-map (or @text "") typeahead-opts dataset-opts
        #(dispatch-event selection-event %)
        #(some-> blur-event (dispatch-event (u/event-value %)))]))))

(comment
  (defn new-search-typeahead
    []
    (let [remote-url (str (data/api-host) "/v1/search")
          wildcard "%QUERY"
          prep-fn (fn [search settings]
                    ;; jquery xhr settings that bloodhound works with
                    (clj->js {:url remote-url
                              :data {:q search}
                              :type "GET"
                              :dataType "json"}))
          bloodhound (inputs/new-bloodhound remote-url wildcard prep-fn)
          bloodhound-with-defaults (fn [q sync async]
                                     (if (string/blank? q)
                                       (sync (clj->js [{:name "Restaurants"}
                                                       {:name "Bars"}
                                                       {:name "Coffee"}]))
                                       (.search bloodhound q sync async)))
          dataset #js {:name "search-dataset"
                       :display "name" ; display the name property from the returned response
                       :source bloodhound-with-defaults}
          typeahead-opts {:minLength 0 ;; required to get defaults to show
                          :highlight true}]
      [inputs/typeahead {:placeholder "Search"} typeahead-opts dataset ::item-selected]))


  ;; JSON post example
  (defn new-users-typeahead
    []
    (let [remote-url (str (data/api-host) "/v1/search/users")
          wildcard "%QUERY"
          prep-fn (fn [search settings]
                    (log/infof "prep-fn called")
                    ;; jquery xhr settings that bloodhound works with
                    (clj->js (-> {:url remote-url
                                  :contentType "application/json"
                                  :data (js/JSON.stringify (clj->js {:q search :role-ids [2 3]}))
                                  :type "POST"
                                  :dataType "json"}
                                 (rpc/with-bearer-auth "eydkj893ja8jdkdj8ajdf"))))
          bloodhound (inputs/new-bloodhound remote-url wildcard prep-fn)
          users-dataset #js {:name "users-dataset" :display "name" :source bloodhound}
          typeahead-opts {:minLength 1 :highlight true}]
      (log/infof "remote url: %s" remote-url)
      [inputs/typeahead {:placeholder "User Search"} typeahead-opts users-dataset e/current-user-selected]))
  )


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
                                                           (let [selected (u/event-value e)
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
                       (let [text (u/event-value e)]
                         (reset! display-ratom text)
                         (dispatch-event text-changed-event text)))]
    (reagent/create-class
     {:display-name "awesomplete"
      :reagent-render (fn [_ _ _]
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
                                                                 (let [key-value (u/key-event-value e)
                                                                       key-target-value (u/event-target-value e)
                                                                       new-value (str key-target-value key-value)]
                                                                   (dispatch-event text-changed-event new-value)))))
                             (init-awesomplete taggle-input-sel
                                                awesomplete
                                                display->item
                                                nil
                                                (fn [x]
                                                  (dispatch-event tag-added-event x {:suggestion? true}))))})))
