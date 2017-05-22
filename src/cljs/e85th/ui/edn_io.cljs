(ns e85th.ui.edn-io
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as coerce]
            [e85th.ui.moment :as moment]
            [e85th.ui.time :as time])
  (:import [goog.date DateTime]))


(extend-protocol IPrintWithWriter
  DateTime
  (-pr-writer [o writer opts]
    (write-all writer "#datetime \"" (time/ts->str o) "\"")))

(when js/moment
  (extend-protocol IPrintWithWriter
    js/moment
    (-pr-writer [o writer opts]
      (write-all writer "#datetime \"" (moment/iso-string o) "\""))))

(if js/moment
  (cljs.reader/register-tag-parser! "datetime" moment/coerce)
  (cljs.reader/register-tag-parser! "datetime" time/date-time))
