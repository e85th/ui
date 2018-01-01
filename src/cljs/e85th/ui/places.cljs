(ns e85th.ui.places
  "Google places functionality."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [e85th.ui.dom :as dom]
            [e85th.ui.util :as u]))

(defn compose-address
  "Takes components of an address and filters out non-blanks and constructs
   a ', ' separated string."
  [street-number street city state zip]
  (let [street-display (cond->> street
                         (not (str/blank? street-number)) (str street-number " "))]
    (->> [street-display city state zip]
         (remove str/blank?)
         (interpose ", " )
         (apply str))))

(defn google-places-script-src
  [callback-fn-name]
  (str "https://maps.googleapis.com/maps/api/js?libraries=places&callback=" callback-fn-name))


(defn new-autocomplete
  "Turns the element-id (string) into an address autocompleter. Returns the google.maps.place.Autocomplete instance.
   You can specify a clojure map of config as described here: https://developers.google.com/places/web-service/autocomplete"
  ([element-id]
   (let [config {:componentRestrictions {:country "us"}}]
     (new-autocomplete element-id config)))
  ([element-id config]
   (let [config (clj->js config)]
     (new google.maps.places.Autocomplete (dom/element-by-id element-id) config))))

(defn add-autocomplete-listener
  "autocomplete is the google.maps.place.Autocomplete instance and callback-fn is the no arg callback function."
  [autocomplete callback-fn]
  (.addListener autocomplete "place_changed" callback-fn))

(defn address-component->map
  "ac is an address component"
  [ac]
  {:long-name (.-long_name ac)
   :short-name (.-short_name ac)
   :types (js->clj (.-types ac))})

(defn place->geocode
  "Parse out the lat and lng from a place returned as a map with keys :lat and :lng."
  [place]
  (if-let [geometry (.-geometry place)]
    (let [loc (.-location geometry)]
      {:lat (.lat loc)
       :lng (.lng loc)})
    {}))

(defn parse-selected-place
  "autocomplete is a google.maps.place.Autocomplete instance.
   Returns nil if an incomplete address is entered.
   Returns a Place map with all optional keys :street-number :street :city
   :state :country :postal-code :lat :lng :formatted-address."
  [autocomplete]
  (let [place (.getPlace autocomplete)
        formatted-address (or (.-formatted_address place) "")
        geocode (place->geocode place)
        address-components (->> place .-address_components (map address-component->map))]
    ;;(log/infof "selected place: %s" geocode)
    ;;(log/infof "selected address comps: %s" address-components)
    (reduce (fn [ans {:keys [long-name short-name types] :as le-place}]
              (cond
                (some #{"street_number"} types) (assoc ans :street-number long-name)
                (some #{"route"} types) (assoc ans :street long-name)
                (some #{"locality"} types) (assoc ans :city long-name)
                (some #{"sublocality"} types) (if (:city ans)
                                                ans
                                                (assoc ans :city long-name))
                (some #{"administrative_area_level_1"} types) (assoc ans :state short-name)
                (some #{"country"} types) (assoc ans :country short-name)
                (some #{"postal_code"} types) (assoc ans :postal-code long-name)
                :else ans))
            (assoc geocode :formatted-address formatted-address)
            address-components)))
