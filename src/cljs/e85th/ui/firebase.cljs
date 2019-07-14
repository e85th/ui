(ns e85th.ui.firebase)

(defn current-user
  "on-user is a function that takes in a user object"
  [on-user]
  (-> (js/firebase.auth)
      (.onAuthStateChanged on-user)))


(defn get-token
  [user on-token]
  (-> user
      .getIdToken
      (.then on-token)))

(defn user->map
  [user]
  {:name user.name
   :email user.email
   :meta-data {:creation-time user.metadata.creationTime
               :last-sign-in-time user.metadata.lastSignInTime}
   :photo-url user.photoURL
   :uid user.uid})

(defn current-user-info
  "on-user is invoked nil if user is not logged in via firebase.
   otherwise a map of keys :name, :email, :photo-url, :uid and :firebase/id-token.
   token can be used to authorize user to the app."
  [on-user]
  (current-user (fn [user]
                  (if-not user
                    (on-user nil)
                    (let [ans (user->map user)]
                      (get-token user (fn [token]
                                        (on-user (assoc ans :firebase/id-token token)))))))))

(defn on-id-token-changed
  "Triggered on login logout, token refresh"
  [on-change]
  (-> (js/firebase.auth)
      (.onIdTokenChanged (fn [user]
                           (if-not user
                             (on-change nil)
                             (let [ans (user->map user)]
                               (get-token user (fn [token]
                                              (on-change (assoc ans :firebase/id-token token))))))))))


;; for determining if user signed up through UserCredential.additionalUserInfo.isNewUser
#_(defn sign-in-with-credential
  "Triggered when a user creates an account."
  [credential]
  (-> (js/firebase.auth)
      (.signInWithCredential credential)
      (.then))
  )
