(ns e85th.ui.transit-io
  (:require [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [e85th.ui.moment :as moment]
            [e85th.ui.util :as u])
  (:import [goog.date DateTime UtcDateTime]))

(def reader
  (transit/reader :json {:handlers {"f" (fn [v] (u/parse-float v nil))
                                    "m" (if moment/Moment
                                          moment/coerce
                                          (fn [s] (UtcDateTime.fromTimestamp s)))}}))

(def datetime-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> v .getTime))
   (fn [v] (-> v .getTime str))))

(def moment-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> v .valueOf))
   (fn [v] (-> v .valueOf str))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; For the transit moment mystery see the following:
;; https://leonid.shevtsov.me/post/how-to-serialize-momentjs-values-with-transit/
;; https://github.com/ianks/moment-transit/blob/master/index.js
(def writer
  (transit/writer :json {:handlers (cond-> {DateTime datetime-writer
                                            UtcDateTime datetime-writer}
                                     moment/Moment (assoc moment/Moment moment-writer)
                                     )}))
