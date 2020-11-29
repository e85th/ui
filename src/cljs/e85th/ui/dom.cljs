(ns e85th.ui.dom)

(defn event-value
  "reads the event target's value"
  [e]
  (-> e .-target .-value))

(defn event-target-value
  "reads the event target's value"
  [e]
  (-> e .-target .-value))

(defn event-target-file-list
  [e]
  (-> e .-target .-files))

(defn event-target-file
  "first selected file"
  [e]
  (-> e event-target-file-list (.item 0)))

(defn event-checked
  "reads the event target's checked property"
  [e]
  (-> e .-target .-checked))

(defn event-target-add-class
  [css-class e]
  (some-> e .-target .-classList (.add css-class)))

(defn event-target-rm-class
  [css-class e]
  (some-> e .-target .-classList (.remove css-class)))

(defn event-prevent-default
  [e]
  (.preventDefault e))

(defn event-stop-propagation
  [^js e]
  (.stopPropagation e))

(defn event-prevent-defaults
  [^js e]
  (event-prevent-default e)
  (event-stop-propagation e))

(defn key-event-code
  [e]
  (.-keyCode e))

(defn key-event-value
  [e]
  (.-key e))

(defn bounding-rect
  "e is the dom element"
  [e]
  (let [r (.getBoundingClientRect e)]
    {:bottom (.-bottom r)
     :height (.-height r)
     :left (.-left r)
     :top (.-top r)
     :width (.-width r)}))


(defn element-by-id
  [id]
  (js/document.getElementById id))

(defn element-style-css-text
  "Returns the css text as a string."
  [id]
  (or (some-> id element-by-id .-style .-cssText)
      ""))

(defn rm-element-by-id
  [id]
  (-> id element-by-id .remove))

(defn element-exists?
  [id]
  (some? (element-by-id id)))

(defn element-value
  [id]
  (some-> id element-by-id .-value))

(defn selected-option-values
  "Answers with a seq of selected option values from an html select element."
  [dom-id]
  (reduce (fn [ans opt]
            (cond-> ans
              (.-selected opt) (conj (.-value opt))))
          []
          (some-> dom-id element-by-id .-options array-seq)))


(defn set-element-value
  [id v]
  (set! (.-value (element-by-id id)) v))


(defn add-event-listener
  [id event-name f]
  (-> (element-by-id id)
      (.addEventListener event-name f)))

(defn remove-event-listener
  [id event-name f]
  (-> (element-by-id id)
      (.removeEventListener event-name f)))


(defn set-interval
  "Sets an interval and returns the ID"
  [f ms]
  (js/window.setInterval f ms))


(defn clear-interval
  [id]
  (js/window.clearInterval id))
