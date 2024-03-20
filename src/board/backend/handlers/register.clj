(ns board.backend.handlers.register
  (:require
    [board.backend.database.users :as user-db]
    [board.util.date-validation :as date-validation]
    [board.util.exception-format :refer [map->invalid-req]]
    [board.util.pformat :refer [pformat]]
    [clojure.tools.logging :as log]
    [farolero.core :as far]
    [farolero.extensions.flow]
    [fmnoise.flow :as flow :refer [then]]
    [malli.core :as malli]
    [malli.error :as malli-error]
    [malli.util :as malli-util]))


(def request-schema
  [:map
   [:username :string]
   [:email :string]
   [:password :string]
   [:date-of-birth
    [:map
     [:year :int]
     [:month :int]
     [:day :int]]]])


(defn check-request-format
  [request]
  (or (malli/validate
        request-schema
        request)
      (far/error
        ::invalid-request-format
        (map->invalid-req
          {:success false
           :message "bad request format"
           :details
           (-> request-schema
               malli-util/closed-schema
               (malli/explain request)
               malli-error/with-spell-checking
               malli-error/humanize)}))))


(defn username-not-too-popular?
  [request]
  (or (>= 10000 (user-db/gen-identifier
                  (:username request)))
      (far/error
        ::username-too-popular
        {:status 409
         :body
         {:success false
          :message "username too popular"}})))


(defn email-not-occupied?
  [request]
  (or (not (seq (user-db/query-email (:email request))))
      (far/error
        ::email-occupied
        (map->invalid-req
          {:success false
           :message "email occupied"}))))


(defn valid-date?
  [request]
  (or (date-validation/valid-date? (:date-of-birth request))
      (far/error
        ::invalid-date
        (map->invalid-req
          {:success false
           :message "invalid date"}))))


(defn user-not-too-old?
  [request]
  (or (date-validation/not-too-old?
        (:date-of-birth request))
      (far/error
        ::user-too-old
        (map->invalid-req
          {:success false
           :message "user too old"
           :error
           {:details
            "no 100+ years old daemons please"}}))))


(defn register-user!
  [request]
  (try
    (user-db/register-user! request)
    (catch Exception e
      (log/error
        "transaction failed! stacktrace: \n"
        (pformat e))
      (far/error
        ::transaction-failed
        {:status 500
         :body
         {:success false
          :message "internal server error"
          :details "please report this error"}}))))


(defn generate-respond
  [request]
  (log/info
    "registered user")
  {:status 201
   :body
   {:success true
    :data (dissoc request :password)}})


(defn register-handler
  [request]
  (log/info "handling register")
  (log/info (:body-params request))
  (far/handler-case
    (let [data (:body-params request)]
      (log/info (pformat data))
      (->> (check-request-format data)
           (then (fn [_] (valid-date? data)))
           (then (fn [_] (user-not-too-old? data)))
           (then (fn [_] (email-not-occupied? data)))
           (then (fn [_] (username-not-too-popular? data)))
           (then (fn [_] (register-user! data)))
           (then (fn [_] (generate-respond data)))))
    (::invalid-request-format
      [_condition return]
      (log/warn "invalid-request-format")
      return)
    (::invalid-date
      [_condition return]
      (log/warn "invalid date")
      return)
    (::user-too-old
      [_condition return]
      (log/warn
        "user is too old \n")
      return)
    (::email-occupied
      [_condition return]
      (log/info "email used")
      return)
    (::username-too-popular
      [_condition return]
      (log/info "username too popular")
      return)
    (::transaction-failed
      [_condition return]
      (log/info "transaction-failed")
      return)))


(comment
  (register-handler
    {:username "john"
     :email "johnabc@who.com"
     :password "abc124"
     :date-of-birth
     {:year 2000
      :month 2
      :day 29}})
  (far/handler-bind 
    [::wha (fn [_] 
             (far/continue))]
    (far/error ::wha)))
