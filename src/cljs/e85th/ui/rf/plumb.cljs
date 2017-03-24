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
   (log/infof "setting container to be %s" div-id)
   (log/infof "container is %s" (u/element-by-id  div-id))
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



(defn add-endpoint
  [pb div opts]
  )


(defn draggable
  [pb dom-id-or-el]
  (.draggable pb dom-id-or-el #js {:containment :parent}))


(defn connect
  "Use the jsPlumb instance pb to connect the two divs
   with ids src-id and target-id."
  [pb src-id target-id]
  (.connect pb #js {:source src-id :target target-id}))
