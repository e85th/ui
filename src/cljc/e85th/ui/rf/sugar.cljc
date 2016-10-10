(ns e85th.ui.rf.sugar
  (:require [re-frame.core]
            [taoensso.timbre :as log]))

(defn keyword-var
  [var-name]
  `(def ~var-name ~(keyword (str *ns*) (str var-name))))

;; nb db# and _# because can't use qualified name as a parameter (syntax quote qualifies everything)
(defmacro defsub
  [sub-name db-path]
  `(do
     ~(keyword-var sub-name)
     (re-frame.core/reg-sub ~sub-name
                            (fn [db# _#]
                              (get-in db# ~db-path)))))

(defmacro def-db-change
  "Registers an event handler for the event-name.  Associates the event
   value into the db-path, then calls the post-change-fn which can be used to run
   validation. post-change-fn gets the db and the event vector."
  ([event-name db-path]
   `(def-db-change ~event-name ~db-path (fn [db# v#] db#)))
  ([event-name db-path post-change-fn]
   `(do
      ~(keyword-var event-name)
      (re-frame.core/reg-event-db ~event-name
                                  (fn [db# v#]
                                    (let [event-value# (last v#)]
                                      (-> (assoc-in db# ~db-path event-value#)
                                          (~post-change-fn v#))))))))
