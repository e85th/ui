(ns e85th.ui.time
  (:refer-clojure :exclude [second])
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as coerce])
  (:import [goog.date Date DateTime]))

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

(defn moment?
  [obj]
  (instance? js/moment obj))

(defn date?
  [obj]
  (instance? js/Date obj))

(defn goog-date?
  [obj]
  (instance? Date obj))

(defn goog-date-time?
  [obj]
  (instance? DateTime obj))

(defn ts->str
  [ts]
  (tf/unparse (tf/formatters :basic-date-time) ts))

(defn date-time
  [dt]
  (cond
    (goog-date-time? dt) dt
    (goog-date? dt) (coerce/from-date dt)
    (date? dt) (coerce/from-date dt)
    (int? dt) (coerce/from-long dt)
    (string? dt) (coerce/from-string dt)
    :else (throw (js/Error. "Don't know how to convert to date: " dt))))

(defn local-date-time
  [dt]
  (coerce/to-local-date-time (date-time dt)))

(def year t/year)
(def month t/month)
(def day t/day)
(def hour t/hour)
(def minute t/minute)
(def second t/second)
(def milli t/milli)
(def now t/now)

(def deconstruct (juxt year month day hour minute second milli))

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

(defn hr+min
  [hr min]
  (+ (* 100 hr) min))

(defn time->hr+min
  "The input is an integer time ie 1230 and returns
   a tuple [hours minutes] [12 30] for the example."
  [time]
  [(int (/ time 100)) (mod time 100)])


(defn extract-time
  "Get the time portion from the date."
  [dt]
  (hr+min (hour dt) (minute dt)))

(defn set-hr-min
  "Returns a DateTime with hours and minutes set to time.
   Coerces dt to DateTime.
   If time is 1430 sets the hr to be 14 and min to be 30."
  ([dt time]
   (let [[h m] (time->hr+min time)]
     (set-hr-min dt h m)))
  ([dt h m]
   (doto (date-time dt)
     (.setHours h)
     (.setMinutes m))))
