(ns e85th.ui.firebase
  (:require [taoensso.timbre :as log]))


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

(defn get-id-token
  [user on-token]
  (-> user
      .getIdToken
      (.then on-token)))


(defn current-user
  "Returns the currently signed in user or null."
  []
  (.-currentUser (auth)))

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
