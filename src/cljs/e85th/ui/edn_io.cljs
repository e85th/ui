(ns e85th.ui.edn-io
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as coerce]
            [cljs.reader]
            [e85th.ui.moment :as moment]
            [e85th.ui.time :as time])
  (:import [goog.date DateTime]))


(extend-protocol IPrintWithWriter
  DateTime
  (-pr-writer [o writer opts]
    (write-all writer "#datetime \"" (time/ts->str o) "\"")))

(cljs.reader/register-tag-parser! "datetime" time/date-time)

(when (exists?  js/moment)
  (extend-protocol IPrintWithWriter
    js/moment
    (-pr-writer [o writer opts]
      (write-all writer "#datetime \"" (moment/iso-string o) "\"")))

  (cljs.reader/register-tag-parser! "datetime" moment/coerce))

;; for graphql ie lacinia returns flatland's ordered/map which is only
;; defined in Clojure, mostly don't care about the order of fields in code
;; except for debugging purposes
(cljs.reader/register-tag-parser! "ordered/map" (partial into {}))
