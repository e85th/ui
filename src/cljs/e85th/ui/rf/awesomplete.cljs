(ns e85th.ui.rf.awesomplete
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log]
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


(def default-lib-opts
  ;; for api case where server sends relevant results filter
  ;; should just return true to include everything in the results
  {:filter (constantly true)})

;; (defn comboplete-view*
;;   [attrs options-sub selected-sub selection-event text-changed-event {:keys [lib-opts] :or {lib-opts default-comboplete-opts} :as opts}]
;;   (let [dom-id                (or (:id attrs)
;;                                   (str (gensym "comboplete-")))
;;         button-dom-id         (str dom-id "-button")
;;         state                 (atom {})
;;         text-changed-listener #(dispatch-event % (:text-changed-event @state))
;;         attrs                 (assoc attrs :id dom-id)
;;         button-click-listener #(comboplete-on-click (:instance @state))]
;;     (reagent/create-class
;;      {:display-name   "comboplete"
;;       ;; use the args passed to reagent-render because they will change based on args changing
;;       :reagent-render (fn [_ _ options-sub selected-sub selection-event text-changed-event opts]
;;                         (swap! state assoc :selection-event (u/as-vector selection-event) :text-changed-event (some-> text-changed-event u/as-vector))
;;                         (let [items     @(rf/subscribe (u/as-vector options-sub))
;;                               selection @(rf/subscribe (u/as-vector selected-sub))
;;                               inst      (:instance @state)]
;;                                         ;(log/infof "suggested items before when %s, dom-id %s" items dom-id)
;;                           (if inst
;;                             (do
;;                               (dom/set-element-value dom-id (or selection ""))
;;                               (set-list inst items))

;;                             (swap! state assoc :first-run-data {:items items :selection selection})))
;;                         ;; always return the same markup for react
;;                         [:span
;;                          [:input attrs]
;;                          [:button {:id    button-dom-id
;;                                    :class "comboplete-dropdown-btn"} "V"]])
;;       :component-did-mount (fn []
;;                              (let [dom-sel                                                 (str "#" dom-id)
;;                                    {:keys [items selection] :or {:items [] :selection ""}} (:first-run-data @state)
;;                                    inst                                                    (new-instance dom-sel lib-opts)]

;;                                (register-select-complete dom-sel #(dispatch-event % (:selection-event @state)))
;;                                (dom/add-event-listener dom-id "change" text-changed-listener)

;;                                (swap! state #(-> (assoc % :instance inst)
;;                                                  (dissoc :first-run-data)))

;;                                (set-list* inst items)
;;                                (dom/set-element-value dom-id selection)
;;                                (dom/add-event-listener button-dom-id "click" button-click-listener)))
;;       :component-will-unmount (fn []
;;                                 (some->> @state :instance destroy)
;;                                 (dom/remove-event-listener dom-id "change" text-changed-listener)
;;                                 (dom/remove-event-listener button-dom-id "click" button-click-listener))})))

;; (defn comboplete
;;   ([options-sub selected-sub selection-event opts]
;;    (comboplete options-sub selected-sub selection-event nil opts))
;;   ([options-sub selected-sub selection-event text-changed-event opts]
;;    (fn [{:keys [tag attrs :as node]}]
;;      (let [attrs (-> attrs
;;                      i/rename-class-attr
;;                      (assoc :add-classes #{"comboplete-input"})
;;                      i/handle-classes)]
;;        [comboplete-view* tag attrs options-sub selected-sub selection-event text-changed-event opts]))))



(defn autocomplete*
  "text-changed-event can be nil. lib-opts in opts are awesomplete options passed to the constructor.
   text-changed-event is fired for user input changes only. If a selection is made then
   only the selection-event is fired. format-fn might be needed for doing html in the suggestion?"
  [attrs
   {:keys [clear-input-on-select? format-fn
           completions-sub display-sub on-select]
    :or   {format-fn identity
           clear-input-on-select? false} :as opts}
   local-ratom]
  ;; (log/info "display-sub " display-sub)
  (let [external-value  (atom "")
        cur-completions (rf/subscribe (u/as-vector completions-sub))
        cur-display     (rf/subscribe (u/as-vector display-sub))

        dom-id                 (or (:id attrs)
                                   (str (gensym "awesomplete-")))
        state                  (atom {})
        selection-fn           (fn [e]
                                 (let [selected                      (dom/event-value e)
                                       {:keys [display->completion]} @state
                                       selection                     (display->completion selected)]
                                   (rf/dispatch (conj (u/as-vector on-select) selection))))
        completions-by-display #(into {} (map (juxt format-fn identity) %))]
    (reagent/create-class
     {:display-name   "awesomplete"
      ;; use the args passed to reagent-render because they will change based on args changing
      :reagent-render (fn [attrs _ _]
                        ;(log/info "cur-display " @cur-display ", external-value " @external-value ", cur-completions " @cur-completions)
                        (when (not= @cur-display @external-value)
                          (reset! external-value @cur-display)
                          (reset! local-ratom @external-value))

                        (let [inst                (:instance @state)
                              display->completion (completions-by-display @cur-completions)]


                          (swap! state assoc :display->completion display->completion)

                          (when inst
                            (dom/set-element-value dom-id (or @local-ratom "")) ; needed otherwise the value doesn't whow up even when :value is used on input
                            (set-list* inst (keys display->completion))))
                        ;; always return the same markup for react
                        [:input attrs])
      :component-did-mount (fn []
                             (let [dom-sel                                       (str "#" dom-id)
                                   {:keys [display->completion display]
                                    :or   {:display->completion {} :display ""}} @state
                                   inst                                          (new-instance dom-sel
                                                                                               (merge default-lib-opts (:lib/opts opts {})))]

                               (register-select-complete dom-sel selection-fn)
                               (swap! state assoc :instance inst)

                               (set-list* inst (keys display->completion))
                               (dom/set-element-value dom-id display)))
      :component-will-unmount #(some->> @state :instance destroy)})))


(defn autocomplete
  [attrs {:keys [on-change] :as opts}]
  (let [local-ratom  (reagent/atom "")
        on-change-fn (fn [e]
                       (let [v (dom/event-value e)]
                         ;; update local-ratom to get display to change
                         ;; don't update re-frame db else whole UI will need to be checked for updates
                         (reset! local-ratom v)
                         (rf/dispatch (conj (u/as-vector on-change) v))))
        dom-id       (or (:id attrs)
                         (str (gensym "awesomplete-")))
        attrs        (assoc attrs :id dom-id :on-change on-change-fn)
        attrs        (i/assoc-class attrs {:+classes #{"awesomplete-input"}})]
    [autocomplete* attrs opts local-ratom]))
