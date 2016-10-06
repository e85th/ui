(ns e85th.ui.rf.fx
  (:require [hodgepodge.core :as hp]
            [re-frame.core :as rf]
            [e85th.ui.notifications :as notify]
            [e85th.ui.util :as u]))

(rf/reg-fx
 :local-storage
 (fn [{:keys [assoc dissoc]}]
   (when dissoc
     (apply dissoc! hp/local-storage dissoc))
   (doseq [[k v] assoc]
     (assoc! hp/local-storage k v))))

(rf/reg-cofx
 :local-storage
 (fn [cofx xs]
   (assoc cofx :local-storage (select-keys hp/local-storage (u/as-vector xs)))))

(rf/reg-fx
 :nav
 (fn [url]
   (u/set-window-location! url)))

(def kind->fn
  {:alert notify/alert
   :info notify/info
   :success notify/success
   :warning notify/warning
   :error notify/error
   :desktop notify/desktop})

(rf/reg-fx
 :notify
 (fn [[kind {:keys [title message]}]]
   (let [f (get kind->fn kind :alert)]
     (f (or title "") message))))
