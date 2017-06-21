(ns e85th.ui.firebase)

(defn current-user
  "on-user is a function that takes in a user object"
  [on-user]
  (-> (js/firebase.auth)
      (.onAuthStateChanged on-user)))

(defn get-token
  [user on-token]
  (-> user
      .getToken
      (.then on-token)))

(defn current-user-info
  "on-user is invoked nil if user is not logged in via firebase.
   otherwise a map of keys :name, :email, :photo-url, :uid and :token.
   token can be used to authorize user to the app."
  [on-user]
  (current-user (fn [u]
                  (if-not u
                    (on-user nil)
                    (let [ans {:name u.name
                               :email u.email
                               :photo-url u.photoURL
                               :uid u.uid}]
                      (get-token u (fn [token]
                                     (on-user (assoc ans :token token)))))))))
