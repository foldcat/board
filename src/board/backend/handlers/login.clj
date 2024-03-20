(ns board.backend.handlers.login
  (:require
    [board.backend.database.users :as users-db]
    [board.backend.session :as session]
    [board.util.exception-format :refer [map->invalid-req]]
    [board.util.pformat :refer [pformat]]
    [clojure.tools.logging :as log]
    [cryptohash-clj.api :as crypto]
    [farolero.core :as far]
    [farolero.extensions.flow]
    [fmnoise.flow :as flow :refer [then]]
    [malli.core :as malli]
    [malli.error :as malli-error]
    [malli.util :as malli-util]))


(defn get-password
  [request]
  (or (ffirst (users-db/query-password (:email request)))
      (far/error ::unknown-cred)))


;; TODO lockdown on too many incorrect attempt
(defn validate-password
  "target is validation target, password is hashed password"
  [target password]
  (or (crypto/verify-with :argon2 target password)
      (far/error ::unknown-cred)))


(defn generate-session
  [ip auto-logout? email]
  (try
    {:status 200
     :body
     {:success true
      :message "success login"
      :token (session/make-session!
               ip
               auto-logout?
               (ffirst (users-db/query-id email)))}}
    (catch Exception e
      (log/error "unknowm error \n" (pformat (Throwable->map e)))
      (far/error
        ::unknown-error
        {:status 500
         :body
         {:success false
          :message "internal server error"
          :details "please report this error"}}))))


(defn login-handler
  [request]
  (far/handler-case
    (let [data (:body-params request)]
      (->> (get-password data)
           (then #(validate-password (:password data) %))
           (then (fn [_]
                   (generate-session
                     (:remote-addr request)
                     (:auto-logout data)
                     (:email data))))))
    (::invalid-request-format
      [_condition return]
      (log/warn "invalid request format")
      return)
    (::unknown-cred
      [_condition]
      (log/warn "unknown cred")
      {:status 401
       :body
       {:success false
        :message
        "unknown email or incorrect password"}})
    (::unknown-error
      [_condition return]
      (log/warn "unknown error")
      return)))
