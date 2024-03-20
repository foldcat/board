(ns board.backend.routes
  (:require
    [board.backend.error :refer [wrap-exception wrap-malform]]
    [board.backend.handlers.dummy :as dummy]
    [board.backend.handlers.echo :as echo]
    [board.backend.handlers.login :as login]
    [board.backend.handlers.register :as register]
    [board.backend.handlers.upload :as upload]
    [board.backend.rate-limit :as limit]
    [board.util.pformat :refer [pformat]]
    [clojure.tools.logging :as log]
    [jsonista.core :as json]
    [muuntaja.core :as muuntaja-core]
    [reitit.coercion :as coercion]
    [reitit.coercion.malli]
    [reitit.coercion.spec :as reitit-coercion-spec]
    [reitit.ring :refer [ring-handler router]]
    [reitit.ring.coercion :as reitit-coercion]
    [reitit.ring.middleware.multipart :refer [multipart-middleware]]
    [reitit.ring.middleware.muuntaja :as wrap-muuntaja]))


(def coercion
  {:muuntaja muuntaja-core/instance
   :compile coercion/compile-request-coercers
   :coercion reitit-coercion-spec/coercion
   :middleware [wrap-malform
                wrap-muuntaja/format-negotiate-middleware
                wrap-muuntaja/format-middleware
                wrap-exception
                reitit-coercion/coerce-request-middleware
                reitit-coercion/coerce-response-middleware]})


(defn app
  [request]
  (limit/wrap-limit request)
  ((ring-handler
     (router
       [["/api"

         ["/auth"
          ["/login"
           {:post {:handler #'login/login-handler
                   :coercion reitit.coercion.malli/coercion
                   :parameters
                   {:body
                    [:map
                     [:email :string]
                     [:password :string]
                     [:auto-logout :boolean]]}}}]
          ["/register"
           {:post {:handler #'register/register-handler
                   :coercion reitit.coercion.malli/coercion
                   :parameters
                   {:body
                    [:map
                     [:username :string]
                     [:email :string]
                     [:password :string]
                     [:date-of-birth
                      [:map
                       [:year :int]
                       [:month :int]
                       [:day :int]]]]}}}]]
         ["/post"
          ["/new"
           {:post {:handler #'upload/upload-handler
                   :middleware [multipart-middleware]}}]]]

        ["/debug"
         ["/dummy" {:get {:handler #'dummy/dummy-handler}}]
         ["/echo" {:post {:handler #'echo/echo}
                   :parameters
                   {:multipart [:map
                                [:file :any]]}
                   :coercion reitit.coercion.malli/coercion
                   :middleware [multipart-middleware]}]]]
       {:data coercion})
     (constantly
       {:status 404
        :headers {"Content-Type" "text/plain"}
        :body "the page does not exist"}))
   request))


(defn app-
  [request]
  (try
    (app request)
    (catch Exception e
      (log/info (pformat (ex-data e))))))


(comment 
  (app {:request-method :post
        :uri "/api/auth/login"})
  (try
   (update
     (app {:request-method :post
           :uri "/api/auth/login"})
     :body 
     #(json/read-value 
        (apply str (map char (.readAllBytes %)))))
   (catch Exception e 
     (ex-data e)))
  (identity muuntaja-core/default-options)
  (muuntaja-core/decode "application/edn" 
                        "{:x 12}"))
