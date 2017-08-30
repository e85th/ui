(ns e85th.ui.rf.macros)

(defn as-vector
  [x]
  (if (vector? x) x [x]))

(defn keyword-var
  [var-name]
  `(def ~var-name ~(keyword (str *ns*) (str var-name))))

;; nb db# and _# because can't use qualified name as a parameter (syntax quote qualifies everything)
(defmacro defsub-db
  [sub-name args & body]
  (assert (vector? args) "defsub args must be a vector.")
  `(do
     ~(keyword-var sub-name)
     (re-frame.core/reg-sub ~sub-name
                            (fn ~args
                              (do ~@body)))))

(defn make-subscription
  [x]
  (let [x (as-vector x)]
    `(re-frame.core/subscribe ~x)))

(defn make-simple-derived-sub
  [sub-name args body]
  (assert (= 2 (count args)) (str "args must be a 2 element vector got: " (count args)))
  (let [subs (mapv make-subscription (first args))]
    `(do
       ~(keyword-var sub-name)
       (re-frame.core/reg-sub ~sub-name
                              (fn [query-v# _#] ~subs)
                              (fn ~args (do ~@body))))))

(defn make-fn-derived-sub
  "This singal-fn is a function that takes in a query-vector and something else.
   It should return a vector of vectors which will be subscribed."
  [sub-name signal-fn body]
  (let [args (first body)
        body (rest body)]
    (assert (vector? args) "Expected a vector representing function args")
    (assert (= 2 (count args)) (str "args must be a 2 element vector got: " (count args)))
    `(do
       ~(keyword-var sub-name)
       (re-frame.core/reg-sub ~sub-name
                              (fn [query-v# x#]
                                (let [subs# (~signal-fn query-v# x#)]
                                  (assert (every? vector? subs#) "Signal-fn must return a vector of vectors. Each vector representing a subscription to be made.")
                                  (mapv re-frame.core/subscribe subs#)))
                              (fn ~args (do ~@body))))))

(defmacro defsub
  ([sub-name db-path]
   `(do
      ~(keyword-var sub-name)
      (re-frame.core/reg-sub ~sub-name
                             (fn [db# _#]
                               (get-in db# ~db-path)))))
  ([sub-name signal-fn-or-args & body]
   (let [f (if (vector? signal-fn-or-args)
             make-simple-derived-sub
             make-fn-derived-sub)]
     (f sub-name signal-fn-or-args body))))

(defmacro defsub-raw
  [sub-name args & body]
  (assert (vector? args) "defsub-raw args must be a vector.")
  `(do
     ~(keyword-var sub-name)
     (re-frame.core/reg-sub-raw ~sub-name
                                (fn ~args
                                  (do ~@body)))))

#_(defmacro defsub-rpc
  [sub-name db-path subs-vec args & body]
  (assert (vector? subs-vec) "defsub-rpc subs-vec must be a vector.")
  (assert (vector? args) "defsub-rpc args must be a vector.")
  (let [[supplied-vals-vec supplied-ev] args
        all-subs (mapv make-subscription subs-vec)]
    (println "all-subs: " all-subs)
    `(do
       ~(keyword-var sub-name)
       (re-frame.core/reg-sub-raw ~sub-name
                                  (fn [db# ev#]
                                    @(reagent.ratom/reaction
                                      (let [sub-vals# (mapv deref ~all-subs)
                                            f# (fn [~supplied-vals-vec ~supplied-ev] ~@body)]
                                        (f# sub-vals# ev#)))
                                    (reagent.ratom/reaction (get-in db# ~db-path)))))))

(defmacro defevent-fx
  "Define the event name as a keyword var and then register the function."
  [event-name args & body]
  (assert (vector? args) "defevent-fx args must be a vector.")
  `(do
     ~(keyword-var event-name)
     (re-frame.core/reg-event-fx ~event-name
                                 (fn ~args
                                   (do ~@body)))))

(defmacro defevent-db
  [event-name args & body]
  (assert (vector? args) "defevent-db args must be a vector.")
  `(do
     ~(keyword-var event-name)
     (re-frame.core/reg-event-db ~event-name
                                 (fn ~args
                                   (do ~@body)))))

(defmacro defevent
  "Registers an event handler for the event-name.  Associates the event
   value into the db-path, then calls the post-change-fn which can be used to run
   validation. post-change-fn gets the db and the event vector."
  ([event-name db-path]
   `(defevent ~event-name ~db-path (fn [db# v#] db#)))
  ([event-name db-path post-change-fn]
   `(do
      ~(keyword-var event-name)
      (re-frame.core/reg-event-db ~event-name
                                  (fn [db# v#]
                                    (let [event-value# (last v#)]
                                      (-> (assoc-in db# ~db-path event-value#)
                                          (~post-change-fn v#))))))))

(comment
  (clojure.pprint/pprint
   (macroexpand-1 `(defsub-rpc engagements m/engagements [subs/current-brand-name]
                     [[current-brand-name] _]
                     (rf/dispatch [e/fetch-engagements current-brand-name]))))

  )
