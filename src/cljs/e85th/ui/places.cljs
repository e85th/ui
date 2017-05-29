(ns e85th.ui.places
  "Google places functionality."
  (:require [schema.core :as s]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [e85th.ui.dom :as dom]
            [e85th.ui.util :as u]))

(s/defn compose-address
  "Takes components of an address and filters out non-blanks and constructs
   a ', ' separated string."
  [street city state zip]
  (->> [street city state zip]
       (remove str/blank?)
       (interpose ", " )
       (apply str)))

(s/defn google-places-script-src
  [callback-fn-name :- s/Str]
  (str "https://maps.googleapis.com/maps/api/js?libraries=places&callback=" callback-fn-name))


(s/defn new-autocomplete
  "Turns the element-id into an address autocompleter. Returns the google.maps.place.Autocomplete instance.
   You can specify a clojure map of config as described here: https://developers.google.com/places/web-service/autocomplete"
  ([element-id :- s/Str]
   (let [config {:componentRestrictions {:country "us"}}]
     (new-autocomplete element-id config)))
  ([element-id :- s/Str config]
   (let [config (clj->js config)]
     (new google.maps.places.Autocomplete (dom/element-by-id element-id) config))))

(s/defn add-autocomplete-listener
  "autocomplete is the google.maps.place.Autocomplete instance and callback-fn is the no arg callback function."
  [autocomplete callback-fn]
  (.addListener autocomplete "place_changed" callback-fn))

(defn address-component->map
  "ac is an address component"
  [ac]
  {:long-name (.-long_name ac)
   :short-name (.-short_name ac)
   :types (js->clj (.-types ac))})

(s/defschema Place
  {(s/optional-key :street-number) s/Str
   (s/optional-key :street) s/Str
   (s/optional-key :street-1) s/Str
   (s/optional-key :city) s/Str
   (s/optional-key :state) s/Str
   (s/optional-key :country) s/Str
   (s/optional-key :postal-code) s/Str
   (s/optional-key :lng) s/Num
   (s/optional-key :lat) s/Num
   (s/optional-key :formatted-address) s/Str})

(s/defn place->geocode
  "Parse out the lat and lng from a place returned as a map with keys :lat and :lng."
  [place]
  (if-let [geometry (.-geometry place)]
    (let [loc (.-location geometry)]
      {:lat (.lat loc)
       :lng (.lng loc)})
    {}))

(s/defn parse-selected-place :- (s/maybe Place)
  "autocomplete is a google.maps.place.Autocomplete instance.
   Returns nil if an incomplete address is entered."
  [autocomplete]
  (let [place (.getPlace autocomplete)
        formatted-address (or (.-formatted_address place) "")
        geocode (place->geocode place)
        ;_ (log/infof "selected place: %s" geocode)
        address-components (->> place .-address_components (map address-component->map))
        ;_ (log/infof "selected address comps: %s" address-components)
        address (reduce (fn [ans {:keys [long-name short-name types] :as le-place}]
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
                        address-components)]
    (when (seq address)
      (assoc address :street-1 (->> ((juxt :street-number :street) address)
                                    (remove nil?)
                                    (interpose " ")
                                    (apply str))))))
