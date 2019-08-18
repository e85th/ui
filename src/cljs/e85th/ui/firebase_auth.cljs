(ns e85th.ui.firebase-auth
  (:require [taoensso.timbre :as log]
            [promesa.async-cljs :refer-macros [async]]
            [promesa.core :as p]))


(defn auth
  []
  (.auth js/firebase))

(defn on-auth-state-changed
  "on-user is a function that takes in a user object"
  [on-user]
  (.onAuthStateChanged (auth) on-user))

(defn on-id-token-changed
  ([on-user on-error]
   (.onIdTokenChanged (auth) on-user on-error)))


(defn ^:export current-user
  "Returns the currently signed in user or null."
  []
  (.-currentUser (auth)))

(defn get-id-token
  ([user on-token]
   (get-id-token user on-token false))
  ([user on-token force?]
   (.then (.getIdToken user force?)
          on-token)))

#_(defn get-user-with-timeout
  ([opts on-user]
   (get-user-with-timeout opts on-user 0))
  ([{:keys [max-n timeout-ms]
     :or {max-n 5 timeout-ms 300}
     :as opts} on-user n]
   (log/infof "get-user-with-timeout called n: %s" n)
   (if-let [user (current-user)]
     (on-user user)
     (if (>= n max-n)
       (on-user nil)
       (js/setTimeout
        #(get-user-with-timeout opts on-user (inc n))
        timeout-ms)))))

(defn get-user-with-timeout
  ([opts on-user]
   (get-user-with-timeout opts on-user 0))
  ([{:keys [max-n timeout-ms on-timeout]
     :or {max-n 5 timeout-ms 300}
     :as opts} on-user n]
   (assert on-timeout "on-timeout required")
   ;(log/infof "get-user-with-timeout called n: %s, user: %s" n (current-user))
   (cond
     (current-user) (on-user (current-user))
     (< n max-n) (js/setTimeout
                  #(get-user-with-timeout opts on-user (inc n))
                  timeout-ms)
     (= n max-n)    (let [timer-id (js/setTimeout on-timeout 2000)]
                      (on-auth-state-changed
                       (fn [user]
                         (when user
                           (js/clearTimeout timer-id)
                           (on-user user)))))
     :else (on-timeout))))

(defn get-id-token-with-timeout
  ([opts on-token]
   (get-user-with-timeout
    opts
    (fn [user]
      (if user
        (get-id-token user on-token)
        (log/debug "get-id-token-with-timeout on user rets nil"))))))

(defn user->map
  [user]
  {:name user.name
   :email user.email
   :meta-data {:creation-time user.metadata.creationTime
               :last-sign-in-time user.metadata.lastSignInTime}
   :photo-url user.photoURL
   :uid user.uid})

(defn handle-auth-state-changed
  "on-user is invoked nil if user is not logged in via firebase.
   otherwise a map of keys :name, :email, :photo-url, :uid and :firebase/id-token.
   token can be used to authorize user to the app."
  [on-user]
  (on-auth-state-changed
   (fn [fb-user]
     (if-not fb-user
       (on-user nil)
       (let [ans (user->map fb-user)]
         (get-id-token
          fb-user
          (fn [token]
            (-> (user->map fb-user)
                (assoc :firebase/id-token token)
                (on-user)))))))))

(defn handle-id-token-changed
  "Triggered on login logout, token refresh"
  [on-change]
  (on-id-token-changed
   (fn [fb-user]
     (if-not fb-user
       (on-change nil)
       (get-id-token
        fb-user
        (fn [token]
          (-> (user->map fb-user)
              (assoc :firebase/id-token token)
              (on-change))))))
   (fn [error]
     (log/error "Encountered error in token changed"))))


;; for determining if user signed up through UserCredential.additionalUserInfo.isNewUser
#_(defn sign-in-with-credential
    "Triggered when a user creates an account."
    [credential]
    (-> (js/firebase.auth)
        (.signInWithCredential credential)
        (.then))
    )


(defn sign-out
  []
  (.signOut (auth)))


(defn auto-refresh-id-token!
  "This needs the Token Service API enabled for the browser API key in use."
  [{:keys [on-token refresh-interval-ms] :as opts}]
  (js/setInterval
   (fn []
     (get-user-with-timeout
      opts
      (fn [user]
        (get-id-token
         user
         on-token
         true))))
   refresh-interval-ms))
