(ns e85th.ui.notifications
  (:require [taoensso.timbre :as log]
            [e85th.ui.util :as u]))



(def toastr js/window.toastr)

(defn- show-toast
  "This function is necessary for the partial defs below to work w/o throwing errors."
  ([f message]
   (show-toast f message ""))
  ([f message title]
   (if f
     (f message title)
     (log/warn "Nil toaster input fn. No toastr available?"))))

(def info (partial show-toast (some-> toastr .-info)))
(def success (partial show-toast (some-> toastr .-success)))
(def warning (partial show-toast (some-> toastr .-warning)))
(def error (partial show-toast (some-> toastr .-error)))

(defn alert
  "Show a toastr alert since it is less invasive otherwise fallback to js alert."
  ([message]
   (alert message ""))
  ([message title]
   (let [message (or message "An unexpected error has occurred.")]
     (if toastr
       (warning message title)
       (js/alert message)))))


(defn- permission
  []
  js/window.Notification.permission)

(defn show*
  [title message]
  (js/window.Notification. title #js {:body message}))

(defn desktop
  "Shows a desktop notification, potentially falling back to a toastr."
  [title message]
  (if (u/notifications-available?)
    (condp = (permission)
      "default" (do
                  (js/window.Notification.requestPermission)
                  (if (= "granted" (permission))
                    (show* title message)
                    (info title message)))
      "denied" (do
                 (log/info "User has disallowed notifications.")
                 (info title message))
      "granted" (show* title message)
      (log/infof "Unhandled permission condition: %s" (permission)))
    (info title message)))
