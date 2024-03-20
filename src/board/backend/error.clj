(ns board.backend.error
  (:require
    [malli.core :as malli]
    [malli.error :as malli-error]
    [reitit.ring.middleware.exception :as exception]))


(def wrap-exception
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {:reitit.coercion/request-coercion
       (fn [exception _request]
         (let [data
               (-> exception
                   Throwable->map
                   :via
                   first
                   :data)

               schema (:schema data)
               value (:value data)

               details
               (-> schema
                   (malli/explain value)
                   malli-error/with-spell-checking
                   malli-error/humanize)]
           {:status 400
            :body
            {:success false
             :message "bad request format"
             :details details}}))})))
