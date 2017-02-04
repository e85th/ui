(ns e85th.ui.time
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as coerce])
  (:import [goog.date DateTime]))

(def short-month-name
  {1 "Jan"
   2 "Feb"
   3 "Mar"
   4 "Apr"
   5 "May"
   6 "Jun"
   7 "Jul"
   8 "Aug"
   9 "Sep"
   10 "Oct"
   11 "Nov"
   12 "Dec"})


(defn date?
  [obj]
  (instance? js/Date obj))

(defn goog-date?
  [obj]
  (instance? DateTime obj))

(defn ts->str
  [ts]
  (tf/unparse (tf/formatters :basic-date-time) ts))

(defn to-date
  [dt]
  (cond
    (goog-date? dt) dt
    (date? dt) (coerce/from-date dt)
    (int? dt) (coerce/from-long dt)
    (string? dt) (coerce/from-string dt)
    :else (throw (js/Error. "Don't know how to convert to date: " dt))))

(def year t/year)
(def month t/month)
(def day t/day)
(def hours t/hours)
(def minutes t/minutes)
(def seconds t/seconds)
(def milli t/milli)
(def now t/now)

(def deconstruct (juxt year month day hours minutes seconds milli))

(defn format
  [formatter dt]
  (tf/unparse formatter dt))

(def format-time (partial format (tf/formatters :hour-minute)))
(def format-date (partial format (tf/formatters :year-month-day)))

(defn format-ts
  "Converts a datetime to a string for display relative to the reference date.
   The as-of-date is the time now for most cases and the formatted date string
   factors into account the as of date to figure out if a more terse representation
   is warranted. For example, an hour ago from now would just show the time eg 3:30 PM"
  ([date]
   (format-ts (now) date))
  ([as-of-date date]
   (if-not date
     ""
     (let [[as-of-year as-of-month as-of-day] (deconstruct as-of-date)
           [year month day] (deconstruct date)
           formatted-time (format-time date)
           month-name (short-month-name month)]
       (cond
         ;; if  today show just the time
         (and (= as-of-year year) (= as-of-month month) (= as-of-day day)) formatted-time
         ;; if same year as now show Feb 22, 5:01 PM
         (= as-of-year year) (str month-name " " day ", " formatted-time)
         ;; otherwise show Feb 22 2015, 5:01 PM
         :else (str month-name " " day ", " year ", " formatted-time))))))
