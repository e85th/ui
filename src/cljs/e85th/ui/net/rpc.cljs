(ns e85th.ui.net.rpc
  (:require [ajax.core :as ajax]
            [e85th.ui.util :as u]))

(def ^:private method-name->method
  {:get ajax/GET
   :post ajax/POST
   :put ajax/PUT
   :delete ajax/DELETE})

(defn new-ajax-request
  "Returns a map that can be used by call to actually make the ajax request."
  [method uri params on-success on-error]
  {:method method
   :uri uri
   :params params
   :handler on-success
   :error-handler on-error})

(defn new-re-frame-request
  [method uri params ok err]
  {:method method
   :uri uri
   :params params
   :on-success (u/as-vector ok)
   :on-failure (u/as-vector err)})

(defn with-json-format
  "Merges in directives to indicate a json request/response and keywords."
  [request]
  (merge {:format (ajax/json-request-format)
          :response-format (ajax/json-response-format {:keywords? true})}
         request))

(defn with-transit-format
  "Merges in directives to indicate a json request/response and keywords."
  [request]
  (merge {:format (ajax/transit-request-format)
          :response-format (ajax/transit-response-format)}
         request))

(defn with-headers
  "merges in headers in the request. headers is a map of String keys and values."
  [request headers]
  (update-in request [:headers] merge headers))

(defn with-bearer-auth
  "Adds the Bearer Authorization header if auth-token is not nil. auth-token is the actual
  token value.  This method will prepend the \"Bearer \" to the auth-token."
  [request auth-token]
  (cond-> request
    auth-token (with-headers {"Authorization" (str "Bearer " auth-token)})))


(defn call
  [req]
  (let [method (-> req :method method-name->method)]
    (assert method (str "Unknown method: " (:method req)))
    (method (:uri req) (dissoc req :method :uri))))
