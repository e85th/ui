(ns e85th.ui.moment
  "Wrapper for moment js."
  (:refer-clojure :exclude [clone second])
  (:require [cljs-time.coerce :as tc])
  (:import [goog.date Date DateTime UtcDateTime]))

(defonce Moment (some-> js/moment .-fn .-constructor))

(def short-month-name
  {0 "Jan"
   1 "Feb"
   2 "Mar"
   3 "Apr"
   4 "May"
   5 "Jun"
   6 "Jul"
   7 "Aug"
   8 "Sep"
   9 "Oct"
   10 "Nov"
   11 "Dec"})

(defn moment?
  "Answers if x is a moment object."
  [x]
  (instance? js/moment x))

(defn date?
  "Answers if x is a js/Date object."
  [x]
  (instance? js/Date x))

(defn goog-date?
  "Answers if x is a goog Date object."
  [x]
  (instance? Date x))

(defn goog-date-time?
  "Answers if x is a goo.date DateTime or UtcDateTime object."
  [x]
  ;; Is the UtcDateTime necessary?
  (or (instance? DateTime x)
      (instance? UtcDateTime x)))

(defn year
  "Answers or sets the year for a moment."
  ([m]
   (assert (moment? m))
   (.year m))
  ([m n]
   (assert (moment? m))
   (.year m n)))

(defn month
  "Answers or sets the month for a moment."
  ([m]
   (assert (moment? m))
   (.month m))
  ([m n]
   (assert (moment? m))
   (.month m n)))

(defn day
  "Answers or sets the day of week for a moment."
  ([m]
   (assert (moment? m))
   (.day m))
  ([m n]
   (assert (moment? m))
   (.day m n)))

(defn date
  "Answers or sets the date for a moment."
  ([m]
   (assert (moment? m))
   (.date m))
  ([m n]
   (assert (moment? m))
   (.date m n)))

(defn hour
  "Answers or sets the hour for a moment."
  ([m]
   (assert (moment? m))
   (.hour m))
  ([m n]
   (assert (moment? m))
   (.hour m n)))


(defn minute
  "Answers or sets the minute for a moment."
  ([m]
   (assert (moment? m))
   (.minute m))
  ([m n]
   (assert (moment? m))
   (.minute m n)))


(defn second
  "Answers or sets the second for a moment."
  ([m]
   (assert (moment? m))
   (.second m))
  ([m n]
   (assert (moment? m))
   (.second m n)))

(defn millisecond
  "Answers or sets the millisecond for a moment."
  ([m]
   (assert (moment? m))
   (.millisecond m))
  ([m n]
   (assert (moment? m))
   (.millisecond m n)))

(def deconstruct (juxt year month date hour minute second millisecond))

(defn moment
  "Constructurs a new moment."
  ([yr]
   (js/moment #js [yr]))
  ([yr month]
   (js/moment #js [yr month]))
  ([yr month date]
   (js/moment #js [yr month date]))
  ([yr month date hr]
   (js/moment #js [yr month date hr]))
  ([yr month date hr minute]
   (js/moment #js [yr month date hr minute]))
  ([yr month date hr minute seconds]
   (js/moment #js [yr month date hr minute seconds]))
  ([yr month date hr minute seconds millis]
   (js/moment #js [yr month date hr minute seconds millis])))


(defn now
  "Returns a moment for the current time."
  []
  (js/moment.))

(defn valid?
  "Answers if m is a valid moment"
  [m]
  (and m (moment? m) (.isValid m)))



(defn iso-string
  "Converts a moment to an iso string."
  [m]
  (assert (moment? m))
  (.toISOString m))

(defn clone
  "Clones a moment."
  [m]
  (.clone m))

(defn add
  "Adds to m n. n is an integer. c is the component as a string ie years, months, days, etc."
  [m n component]
  (.add (clone m) n component))

(defn subtract
  "subtracts from m n. n is an integer. c is the component as a string ie years, months, days, etc."
  [m n c]
  (.subtract (clone m) n c))

(defn goog-date-time
  "Converts m to a goog DateTime"
  [m]
  (let [[yr mnth day hr minute seconds millis] (deconstruct m)]
    (DateTime. yr mnth day hr minute seconds millis)))

(defn goog-date
  "Converts m to a goog Date"
  [m]
  (let [[yr mnth day] (deconstruct m)]
    (Date. yr mnth day)))


(defn set-time
  "Returns a new moment with the hr and minutes set as specified"
  [dt hr minute]
  (doto (clone dt)
    (.hours hr)
    (.minutes minute)))

(defn set-date
  "Returns a new moment with the year month and date set as specified."
  [dt yr month date]
  (doto (clone dt)
    (.year yr)
    (.month month)
    (.date date)))

(defn coerce
  "Coerce to a moment from either string, integer, date, goog Date or goog DateTime.
   Returns nil if not coerceible."
  [x]
  (if (moment? x)
    x
    (cond
      (or (string? x)
          (integer? x)
          (date? x)) (js/moment. x)
      (or (goog-date? x)
          (goog-date-time? x)) (js/moment. (.getTime x)))))

(defn coercible?
  "Tests if x is coercible."
  [x]
  (some? (coerce x)))

(defn utc
  "Convert m to utc."
  [m]
  (assert (moment? m))
  (js/moment.utc m))

(defn tz
  "Convert m to timezone. timezone is a string like America/New_York"
  [m timezone]
  (js/moment.tz m timezone))

(defn equal?
  "Tests if two moments are equal."
  [m-1 m-2]
  (and m-1 m-2 (.isSame m-1 m-2)))

(defn guess-tz
  []
  (-> js/moment .-tz .guess))

(defn format
  ([m]
   (assert (moment? m))
   (.format m))
  ([m format-str]
   (assert (moment? m))
   (.format m format-str))
  ([m format-str timezone]
   (assert (moment? m))
   (format (tz m timezone) format-str)))

(defn format-time
  "Formats and returns the time portion of the moment instance as a string."
  [m]
  (format m "h:mm a"))

(defn format-ts
  "Converts a datetime to a string for display relative to the reference date.
   The as-of-date is the time now for most cases and the formatted date string
   factors into account the as of date to figure out if a more terse representation
   is warranted. For example, an hour ago from now would just show the time eg 3:30 PM
   Takes care of coercions"
  ([m]
   (format-ts m (guess-tz)))
  ([m tz]
   (format-ts (now) m tz))
  ([as-of-m m t-zone]
   (if-not m
     ""
     (let [m (tz (coerce m) t-zone)
           [as-of-year as-of-month as-of-day] (deconstruct as-of-m)
           [year month day] (deconstruct m)
           formatted-time (format-time m)
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
  [m]
  (hr+min (hour m) (minute m)))

(defn set-hr-min
  "Returns a DateTime with hours and minutes set to time.
   Coerces dt to DateTime.
   If time is 1430 sets the hr to be 14 and min to be 30."
  ([m time]
   (let [[hr mn] (time->hr+min time)]
     (set-hr-min m hr mn)))
  ([m hr mn]
   (-> (clone m) (hour hr) (minute mn))))


(defn millis
  [m]
  (.valueOf m))

(defn to-date-time
  "Convert to datetime"
  [m]
  (-> m millis tc/from-long))

(defn- zp2
  [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defn- zp3
  [n]
  (cond
    (< n 10) (str "00" n)
    (< n 100) (str "0" n)
    :else (str n)))

(defn str-no-tz
  [yr month date hr min secs ms]
  (str yr "-" (zp2 month) "-" (zp2 date) " " (zp2 hr) ":" (zp2 min) ":" (zp2 secs) "." (zp3 ms)))
