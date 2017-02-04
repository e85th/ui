(ns e85th.ui.edn-io
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as coerce]
            [e85th.ui.time :as time])
  (:import [goog.date DateTime]))


;; (defmethod print-method DateTime [this w]
;;   (.write w "#datetime \"")
;;   (.write w (time/ts->str this))
;;   (.write w "\""))

(cljs.reader/register-tag-parser! "datetime" time/to-date)
