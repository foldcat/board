(ns board.backend.error
  (:require
    [board.util.pformat :refer [pformat]]
    [malli.core :as malli]
    [malli.error :as malli-error]
    [muuntaja.core :as muuntaja]
    [reitit.ring.middleware.exception :as exception]
    [taoensso.timbre :as log]))


(defn handle-invalid-data
  "execute when data doesn't match schema"
  [exception _request]
  (let [data (ex-data exception)
        schema (:schema data)
        value (:value data)

        details
        (-> schema
            (malli/explain value)
            malli-error/with-spell-checking
            malli-error/humanize)]
    (log/info (pformat (ex-data exception)))
    {:status 400
     :body
     {:success false
      :message "bad request format"
      :details details}}))


(defn handle-malformed-request
  [_exception request]
  (log/info (pformat request))
  (let [accept (get (:headers request) "accept")

        processed
        (if (#{"application/json"
               "application/edn"
               "application/transit+msgpack"
               "application/transit+json"}
             accept)
          accept
          "application/json")]

    {:status 400
     :headers {"Content-Type" "application/json"}
     :body
     (muuntaja/encode
       processed
       {:success false
        :message "malformed request"})}))


(def wrap-exception
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {:reitit.coercion/request-coercion
       handle-invalid-data})))


(def wrap-malform
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {:muuntaja/decode
       handle-malformed-request})))
