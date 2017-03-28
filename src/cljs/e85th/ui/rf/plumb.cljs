(ns e85th.ui.rf.plumb
  "NB. Using vanilla JS is better instead of the jQuery one."
  (:require [reagent.core :as reagent]
            [e85th.ui.util :as u]
            [taoensso.timbre :as log]))

(defn new-instance
  ([]
   (js/jsPlumb.getInstance))
  ([default-opts]
   (js/jsPlumb.getInstance (clj->js default-opts))))

(defn container
  ([pb]
   (.getContainer pb))
  ([pb div-id]
   (.setContainer pb (u/element-by-id  div-id))))

(defn control
  ([div-opts init-data on-mount-fn]
   (control div-opts init-data on-mount-fn (constantly nil)))
  ([div-opts init-data on-mount-fn on-unmount-fn]
   (let [pb (new-instance init-data)
         div-opts (merge {:id (str (gensym "js-plumb-container-"))} div-opts )]
     (reagent/create-class
      {:display-name "js-plumb"
       :reagent-render (fn [_ _ _ _]
                         [:div div-opts])
       :component-did-mount (fn []
                              (container pb (:id div-opts))
                              (on-mount-fn pb))
       :component-will-unmount (fn []
                                 (on-unmount-fn pb))}))))

(defn draggable
  [pb dom-id-or-el]
  (.draggable pb dom-id-or-el #js {:containment :parent}))

(defn connect
  "Use the jsPlumb instance pb to connect the two divs
   with ids src-id and target-id."
  [pb src-id target-id]
  (.connect pb #js {:source src-id :target target-id}))

(defn make-source
  [pb dom-id opts]
  (let [el (u/element-by-id dom-id)
        opts (clj->js opts)]
    (.makeSource pb el opts)))

(defn make-target
  [pb dom-id opts]
  (let [el (u/element-by-id dom-id)
        opts (clj->js opts)]
    (.makeTarget pb el opts)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Connections
(defn connections
  [pb]
  (map (fn [c]
         {:src (-> c .-sourceId) :tgt (-> c .-targetId)})
       (.getAllConnections pb)))

(defn inbound-connections
  [pb dom-id]
  (-> pb (.getConnections (clj->js {:target dom-id}))))

(defn outbound-connections
  [pb dom-id]
  (-> pb (.getConnections (clj->js {:source dom-id}))))

(defn detach-connection
  [pb cn]
  (.detach pb cn))

(defn rm-inbound-connections
  [pb dom-id]
  (doseq [cn (inbound-connections pb dom-id)]
    (detach-connection pb cn)))

(defn detach-endpoint-connections
  [pb dom-id]
  (.detachAllConnections pb dom-id))


(defn rm-endpoint
  [pb dom-id]
  (detach-endpoint-connections pb dom-id)
  (.deleteEndpoint pb dom-id))

(defn rm
  [pb dom-id]
  (.remove pb dom-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events
(defn- register-event-handler
  [pb event cb]
  ;; fyi: bind works but .on does not
  (.bind pb event cb))

(defn register-connection-click-handler
  [pb cb]
  "Register connection click listener. The callback function receives the connection."
  (register-event-handler pb "click" cb))

(defn register-connection-created-handler
  "Register connection created listener. Callback function
   receives the connection info and the original event."
  [pb cb]
  (register-event-handler pb "connection" cb))


(defn connection-as-map
  "Returns source and target dom ids as a map."
  [cn]
  {:source-id (.-sourceId cn)
   :target-id (.-targetId cn)})
