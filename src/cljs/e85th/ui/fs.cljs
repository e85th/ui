(ns e85th.ui.fs
  (:require [goog.labs.format.csv :as csv]))

(defn read-text-file
  [file on-file-read]
  (let [rdr (js/FileReader.)]
    (set! (.-onload rdr)
          (fn [e]
            (-> e .-target .-result on-file-read)))
    (.readAsText rdr file)))

(defn read-csv
  "on-data is called when the parsed csv data, on-error is called with the error
   object when an error is encountered. "
  ([file on-data on-error]
   (read-csv file on-data on-error {}))
  ([file on-data on-error {:keys [clojureize?] :or {clojureize? true} :as opts}]
   (let [cb (fn [file-content-str]
              (try
                (on-data (cond-> (csv/parse file-content-str)
                           clojureize? js->clj))
                (catch js/Error err
                  (on-error err))))]
     (read-text-file file cb))))
