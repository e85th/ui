(ns e85th.ui.rf.sugar
  (:require [re-frame.core]
            [taoensso.timbre :as log]))

(defmacro defsub
  [sub-name db-path]
  `(do
     (def ~sub-name ~(keyword (str *ns*) (str sub-name)))
     (re-frame.core/reg-sub ~sub-name
                            (fn [db# _#]
                              (get-in db# ~db-path)))))
