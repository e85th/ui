(ns e85th.ui.net.rpc
  (:require [ajax.core :as ajax]))

(def ^:private method-name->method
  {:get ajax/GET
   :post ajax/POST
   :put ajax/PUT
   :delete ajax/DELETE})

(defn new-ajax-request
  "Returns a map that can be used by call to actually make the ajax request."
  [uri method on-success on-error]
  {:uri uri
   :method method
   :handler on-success
   :error-handler on-error})

(defn with-json-format
  "Merges in directives to indicate a json request/response and keywords."
  [request]
  (merge {:format :json
          :response-format :json
          :keywords? true}
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
