(ns e85th.ui.transit-io
  (:require [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [e85th.ui.util :as u]))

(def reader
  (transit/reader :json {:handlers {"f" (fn [v] (u/parse-float v nil))}}))
