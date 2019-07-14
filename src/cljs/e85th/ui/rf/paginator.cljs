(ns e85th.ui.rf.paginator
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.util :as u]
            [devcards.core :as d :refer-macros [defcard-rg]]
            ;[kioo.reagent :as k :refer-macros [defsnippet]]
            ))


;; (defsnippet paginator* "templates/e85th/ui/rf/paginator.html" [:.paginator]
;;   [{:keys [current-page total-pages first-page? last-page?]} on-page-cb]
;;   {[:.paginator__first-page] (if first-page?
;;                                (k/set-attr :disabled true)
;;                                (k/listen :on-click #(on-page-cb 1)))
;;    [:.paginator__previous-page] (if first-page?
;;                                   (k/set-attr :disabled true)
;;                                   (k/listen :on-click #(on-page-cb (dec current-page))))
;;    [:.paginator__next-page] (if last-page?
;;                               (k/set-attr :disabled true)
;;                               (k/listen :on-click #(on-page-cb (inc current-page))))
;;    [:.paginator__last-page] (if total-pages
;;                               (if last-page?
;;                                 (k/set-attr :disabled true)
;;                                 (k/listen :on-click #(on-page-cb total-pages)))
;;                               (k/substitute ""))
;;    [:.paginator__current-page] (k/content current-page)
;;    [:.paginator__total-pages] (if total-pages
;;                                 (k/content (str " of " total-pages))
;;                                 (k/substitute ""))})

;; (defn paginator
;;   "Displays a paginator. The sub should return a map with required key :current-page.
;;    Key :total-pages is optional."
;;   [paginator-sub page-selection-event]
;;   (let [paginator-info (rf/subscribe (u/as-vector paginator-sub))
;;         on-page-cb (fn [page]
;;                      (rf/dispatch (conj (u/as-vector page-selection-event) page)))
;;         enrich-fn (fn [{:keys [current-page total-pages] :or {current-page 1} :as info}]
;;                     (merge info {:current-page current-page
;;                                  :first-page? (= 1 current-page)
;;                                  :last-page? (true? (and total-pages (= total-pages current-page)))}))]
;;     (fn [_ _]
;;       [paginator* (enrich-fn @paginator-info) on-page-cb])))
