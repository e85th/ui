(ns e85th.ui.net.websockets
  "Push events from the server are handled here and dispatched.  There are two multimethods, clients should use
  only the on-event multi method.  The sente-event-handler is for internal use. Using register-listener! allows
  multiple bits of code to respond to an event, which is useful if you are embedding one application inside another
  as a React component for example."
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]))

(defonce event-router (atom nil))
(defonce chsk-info (atom nil))

(defonce fan-out-fns (atom []))

(defn register-listener!
  "Adds the on-event-fn to the list of functions to be called when a new
   server push event arrives. The on-event-fn takes a variant with the event keyword as the first
   item in the vector. eg [:event/id {:foo 42}]"
  [on-event-fn]
  (assert on-event-fn "nil on-event-fn")
  (swap! fan-out-fns conj on-event-fn))

(defn fanout-event
  "Calls each registered listener to handle the event."
  [event-data]
  (doseq [ws-fn @fan-out-fns]
    (try
      (ws-fn event-data)
      (catch js/Error ex
        (log/errorf "%s" ex))
      (catch :default ex ;; JS allows you to throw anything
        (log/errorf "Something threw a non-error %s" ex)))))

;; sente-event-handler receives a map with keys :id, :?data and :event
;; :id will be one of the :chsk/recv etc. ?data is the actual data the
;; server pushed such as [:event/id {:foo 42}]
(defmulti sente-event-handler :id) ; dispatch on event id

(defmethod sente-event-handler :chsk/state
  [{:keys [?data] :as msg}]
  (if (get-in ?data [1 :first-open?] false)
    (log/infof "Channel socket successfully established!")
    (log/infof "Channel socket state changed.")))

(defmethod sente-event-handler :chsk/recv
  [{:keys [?data] :as msg}]
  (fanout-event ?data))

(defmethod sente-event-handler :chsk/handshake
  [{:keys [?data] :as msg}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log/infof "Sente handshake: %s" ?data)))

(defmethod sente-event-handler :default
  [{:keys [event] :as msg}]
  (log/infof "Unhandled sente event: %s" event))

(defn stop-router!
  []
  (when-let [stop-fn @event-router]
    (log/info ";; Stopping Sente websocket router.")
    (stop-fn)))

(defn start-router!
  []
  (stop-router!)
  (log/info ";; Starting Sente websocket router.")
  (reset! event-router (sente/start-chsk-router! (:ch-recv @chsk-info) sente-event-handler)))

(defn init!
  "Takes a path and a map of options see sente docs."
  [path sente-opts]
  (reset! chsk-info (sente/make-channel-socket! path sente-opts))
  (start-router!))

(defn send-message!
  "msg has to be a vector where the first item is a namespaced keyword. sente enforces this."
  [msg]
  (if-some [send-fn (some-> chsk-info deref :send-fn)]
    (send-fn msg)
    (throw (ex-info "No send-fn. Were websockets initialized?" {}))) )
