(ns e85th.ui.rf.tree
  (:refer-clojure :exclude [create-node])
  (:require [reagent.core :as reagent]))

(defn tree*
  [sel]
  ;; get existing instance, don't create a new one.
  (-> sel js/$ (.jstree true)))

;dom-id (str (gensym "js-tree-"))
(defn js-tree
  ([dom-id init-data]
   (js-tree dom-id init-data (constantly nil)))
  ([dom-id init-data on-mount-fn]
   (reagent/create-class
    {:display-name "js-tree"
     :reagent-render (fn []
                       [:div {:id dom-id}])
     :component-did-mount (fn []
                            (-> (str "#" dom-id) js/$ (.jstree (clj->js init-data)))
                            (when on-mount-fn
                              (on-mount-fn)))
     :component-will-unmount (fn []
                               (-> (str "#" dom-id) tree* .destroy))})))

(defn create-node
  "Add a regular node to the tree. node is a map."
  [tree-sel parent-id node]
  (-> tree-sel tree* (.create_node parent-id (clj->js node))))

(defn create-root-node
  "Add a root node to the tree."
  [tree-sel node]
  (create-node tree-sel "#" node))

(defn normalize-node
  [n]
  (cond-> n
    (not (map? n)) (js->clj :keywordize-keys true)))

;; gets the selected node in the tree if any otherwise nil
(defn selection
  [tree-sel]
  "Gets the selected node in the tree if any otherwise nil"
  (-> tree-sel tree* .get_selected first normalize-node))

(defn node
  "Returns the node if any otherwise returns nil"
  [tree-sel node-id]
  (let [n (-> tree-sel tree* (.get_node node-id))]
    ;; NB .get_node returns false if node is not found.
    (when-not (false? n)
      (normalize-node n))))

(defn original-node
  "Returns the node's original node data with the :type keywordized.
   If the input node does not have an :original entry such as the root,
   then the returned value is nil."
  [n]
  (some-> n
          normalize-node
          :original
          (update-in [:type] keyword)))

(defn node?
  [tree-sel node-id]
  (some? (node tree-sel node-id)))


(def root-id? (partial = "#"))

(def ^{:doc "If the id is '#'. input node can be a js obj or clj map."}
  root? (comp root-id? :id normalize-node))

(defn rename-node
  [tree-sel node-id new-node-name]
  (-> tree-sel tree* (.rename_node node-id new-node-name)))

(defn rm-node
  [tree-sel node-id]
  (-> tree-sel tree* (.delete_node node-id)))

(defn mv-node
  [tree-sel node-id parent-node-id]
  (-> tree-sel tree* (.move_node node-id parent-node-id)))

(defn trigger-load
  "Triggers the ajax loading of the node's children specified by the node-id."
  [tree-sel node-id]
  (-> tree-sel tree* (.load_node node-id)))

(defn refresh
  "This will trigger reloading the whole tree."
  [tree-sel]
  (-> tree-sel tree* .refresh))

(defn node-type [node]
  (-> node .-type keyword))

(defn node-moved-data
  "Given the event for when a node moves returns map of :parent :old-parent and :node."
  [event-data]
  {:parent (-> event-data .-parent)
   :old-parent (-> event-data .-old_parent)
   :node (normalize-node (-> event-data .-node))})

(defn node-text
  [tree-sel node-id]
  (-> (node tree-sel node-id) :text))

(defn dnd-event-coordinates
  "Returns with a map of :clientX, :clientY, :offsetX and :offsetY.
   data is the data from the vataka dnd callback events. This only works with
   non-html5 events."
  [dnd-data]
  (let [e (-> dnd-data .-event)
        cx (-> e .-clientX)
        cy (-> e .-clientY)
        ox (-> e .-offsetX)
        oy (-> e .-offsetY)]
    {:client-x cx :client-y cy
     :offset-x ox :offset-y oy}))


(defn jstree-dnd?
  "Is jstree the source of the event data?"
  [dnd-data]
  (some? (some->> dnd-data .-data .-jstree)))

(defn dnd-data
  [data]
  (js->clj (.-data data) :keywordize-keys true))

(defn dnd-data-nodes
  [data]
  (:nodes (dnd-data data)))

;;-----------------------------------------------------------------------
;; When a node is dragged the dnd-data given via the callback has a
;; helper function which can be invoked to change the x to a checkmark
;; and vice versa to indicate what's appropriate to drag and drop
;; on an element.
;;-----------------------------------------------------------------------
(defn show-dnd-drag-status
  [dnd-data valid?]
  (let [rm-class (if valid? "jstree-err" "jstree-ok")
        add-class (if (= "jstree-err" rm-class) "jstree-ok" "jstree-err")]
    (-> dnd-data .-helper (.find ".jstree-icon") (.removeClass rm-class) (.addClass add-class))))


(defn- register-event-handler
  [tree-sel event-name handler]
  (-> tree-sel js/$ (.on event-name handler)))

(defn- register-document-event-handler
  [event-name handler]
  (-> js/document js/$ (.on event-name handler)))

(defn register-activate-node-handler
  [tree-sel handler]
  (register-event-handler tree-sel "activate_node.jstree" handler))

(defn register-rename-node-handler
  [tree-sel handler]
  (register-event-handler tree-sel "rename_node.jstree" handler))

(defn register-create-node-handler
  [tree-sel handler]
  (register-event-handler tree-sel "create_node.jstree" handler))

(defn register-move-node-handler
  [tree-sel handler]
  (register-event-handler tree-sel "move_node.jstree" handler))

(defn register-dnd-start-handler
  [handler]
  (register-document-event-handler "dnd_start.vakata" handler))

(defn register-dnd-move-handler
  [handler]
  (register-document-event-handler "dnd_move.vakata" handler))
