(ns e85th.ui.google-oauth)

(set! *warn-on-infer* true)

(defn google-user->map
  "google-user is a google user object."
  [^js/gapi.auth2.GoogleUser google-user]
  (let [^js/gapi.auth2.BasicProfile p (.getBasicProfile google-user)
        ^js/gapi.auth2.AuthResponse ar (.getAuthResponse google-user)]
    {:id (.getId p)
     :name (.getName p)
     :given-name (.getGivenName p)
     :family-name (.getFamilyName p)
     :email (.getEmail p)
     :image-url (.getImageUrl p)
     :token (.-id_token ar)}))


(defn sign-out
  "on-sign-out is a zero arity function invoked when the user is signed out."
  ([]
   (sign-out (constantly nil)))
  ([on-sign-out]
   (let [^js/Promise p (js/gapi.auth2.getAuthInstance.signOut)]
     (.then p on-sign-out))))

(defn signed-in?
  "Answers true if the user is signed in or false if not."
  []
  (js/gapi.auth2.GoogleAuth.isSignedIn.get))

(defn current-user
  []
  (js/gapi.auth2.GoogleAuth.currentUser.get))

(defn current-user-listen
  "listener is a fn that takes a GoogleUser parameter.  listener is passed
   a GoogleUser on every change that modifies the currentUser."
  [listener]
  (js/gapi.auth2.GoogleAuth.currentUser.listen listener))
