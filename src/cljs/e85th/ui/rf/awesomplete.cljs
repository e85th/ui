(ns e85th.ui.rf.awesomplete
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log]
            [devcards.core :as d :refer-macros [defcard defcard-rg]]
            [e85th.ui.dom :as dom]
            [e85th.ui.rf.inputs :as i]
            [e85th.ui.util :as u]))

(defn new-instance
  "Creates a new instance based on the dom-sel and opts map."
  [dom-sel opts]
  (js/Awesomplete. (.querySelector js/document dom-sel) (clj->js opts)))




(defn evaluate
  [inst]
  (.evaluate inst))

(defn close
  [inst]
  (.close inst))

(defn open
  [inst]
  (.open inst))

(defn destroy
  [inst]
  (when inst
    (.destroy inst)))

(defn set-list*
  "Sets the list property for the awesomplete instance. xs are convereted to js objs."
  [inst xs]
  (set! (.-list inst) (clj->js xs)))

(defn set-list
  "Sets and evaluates the data the instance is working with."
  [inst xs]
  ;; must call after setting list property to regenerate the list
  ;; close it otherwise the box is open with the selected item highlighted
  (doto inst
    (set-list* xs)
    (evaluate)
    (close)))

(defn register-callback
  "cb is a 1 arity function that gets the event as the argument"
  [event-name dom-sel cb]
  (.on (js/$ dom-sel) event-name cb))

(def register-select-complete (partial register-callback "awesomplete-selectcomplete"))

(defn- comboplete-on-click
  [combo]
  (let [len (some-> combo .-ul .-childNodes .-length)]
    (if (and len (zero? len))
      (do
        (set! (.-minChars combo) 0)
        (evaluate combo))
      (if (some-> combo .-ul (.hasAttribute "hidden"))
        (open combo)
        (close combo)))))


(defn- dispatch-event
  [e rf-event]
  ;(log/infof "dispatch-event: %s, %s" e rf-event)
  (when rf-event
    (rf/dispatch (conj rf-event (dom/event-value e)))))


(def default-comboplete-opts
  ;{:filter (constantly true)}
  {})

(defn comboplete-view*
  [tag attrs options-sub selected-sub selection-event text-changed-event {:keys [lib-opts] :or {lib-opts default-comboplete-opts} :as opts}]
  (let [dom-id (or (:id attrs)
                   (str (gensym "comboplete-")))
        button-dom-id (str dom-id "-button")
        state (atom {})
        text-changed-listener #(dispatch-event % (:text-changed-event @state))
        attrs (assoc attrs :id dom-id)
        button-click-listener #(comboplete-on-click (:instance @state))]
    (reagent/create-class
     {:display-name "comboplete"
      ;; use the args passed to reagent-render because they will change based on args changing
      :reagent-render (fn [_ _ options-sub selected-sub selection-event text-changed-event opts]
                        (swap! state assoc :selection-event (u/as-vector selection-event) :text-changed-event (some-> text-changed-event u/as-vector))
                        (let [items @(rf/subscribe (u/as-vector options-sub))
                              selection @(rf/subscribe (u/as-vector selected-sub))
                              inst (:instance @state)]
                                        ;(log/infof "suggested items before when %s, dom-id %s" items dom-id)
                          (if inst
                            (do
                              (dom/set-element-value dom-id (or selection ""))
                              (set-list inst items))

                            (swap! state assoc :first-run-data {:items items :selection selection})))
                        ;; always return the same markup for react
                        [:span
                         [tag attrs]
                         [:button {:id button-dom-id
                                   :class "comboplete-dropdown-btn"} "V"]])
      :component-did-mount (fn []
                             (let [dom-sel (str "#" dom-id)
                                   {:keys [items selection] :or {:items [] :selection ""}} (:first-run-data @state)
                                   inst (new-instance dom-sel lib-opts)]

                               (register-select-complete dom-sel #(dispatch-event % (:selection-event @state)))
                               (dom/add-event-listener dom-id "change" text-changed-listener)

                               (swap! state #(-> (assoc % :instance inst)
                                                 (dissoc :first-run-data)))

                               (set-list* inst items)
                               (dom/set-element-value dom-id selection)
                               (dom/add-event-listener button-dom-id "click" button-click-listener)))
      :component-will-unmount (fn []
                                (some->> @state :instance destroy)
                                (dom/remove-event-listener dom-id "change" text-changed-listener)
                                (dom/remove-event-listener button-dom-id "click" button-click-listener))})))

(defn comboplete
  ([options-sub selected-sub selection-event opts]
   (comboplete options-sub selected-sub selection-event nil opts))
  ([options-sub selected-sub selection-event text-changed-event opts]
   (fn [{:keys [tag attrs :as node]}]
     (let [attrs (-> attrs
                     i/rename-class-attr
                     (assoc :add-classes #{"comboplete-input"})
                     i/handle-classes)]
       [comboplete-view* tag attrs options-sub selected-sub selection-event text-changed-event opts]))))



(defn- suggestions-view*
  "text-changed-event can be nil. lib-opts in opts are awesomplete options passed to the constructor.
   text-changed-event is fired for user input changes only. If a selection is made then
   only the selection-event is fired. format-fn might be needed for doing html in the suggestion?"
  [tag attrs suggestions-sub display-sub selection-event text-changed-event
   {:keys [lib-opts clear-input-on-select? format-fn] :or {format-fn identity
                                                           clear-input-on-select? false
                                                           lib-opts {}}}]
  (let [dom-id (or (:id attrs)
                   (str (gensym "awesomplete-")))
        state (atom {})
        on-change-fn #(dispatch-event % (:text-changed-event @state))
        attrs (assoc attrs :id dom-id :on-change on-change-fn)
        selection-fn (fn [e]
                       (let [selected (dom/event-value e)
                             {:keys [display->item selection-event]} @state
                             selection (display->item selected)]
                         (rf/dispatch (conj selection-event selection))))
        items-by-display #(into {} (map (juxt format-fn identity) %))]
    (reagent/create-class
     {:display-name "awesomplete"
      ;; use the args passed to reagent-render because they will change based on args changing
      :reagent-render (fn [_ _ suggestions-sub display-sub selection-event text-changed-event opts]
                        (let [items @(rf/subscribe (u/as-vector suggestions-sub))
                              display @(rf/subscribe (u/as-vector display-sub))
                              inst (:instance @state)
                              display->item (items-by-display items)]

                          (swap! state assoc
                                 :selection-event (u/as-vector selection-event)
                                 :text-changed-event (u/as-vector text-changed-event)
                                 :display->item display->item)

                          (when inst
                            (dom/set-element-value dom-id (or display "")) ; needed otherwise the value doesn't whow up even when :value is used on input
                            (set-list* inst (keys display->item))))
                        ;; always return the same markup for react
                        [tag attrs])
      :component-did-mount (fn []
                             (let [dom-sel (str "#" dom-id)
                                   {:keys [display->item display] :or {:display->item {} :display ""}} @state
                                   inst (new-instance dom-sel lib-opts)]

                               (register-select-complete dom-sel selection-fn)
                               (swap! state assoc :instance inst)

                               (set-list* inst (keys display->item))
                               (dom/set-element-value dom-id display)))
      :component-will-unmount #(some->> @state :instance destroy)})))

(defn suggestions
  [suggestions-sub display-sub selection-event text-changed-event opts]
  (fn [{:keys [tag attrs :as node]}]
    (let [attrs (-> attrs
                    i/rename-class-attr
                    (assoc :add-classes #{"awesomplete-input"})
                    i/handle-classes)]

      [suggestions-view* tag attrs suggestions-sub display-sub selection-event text-changed-event opts])))
