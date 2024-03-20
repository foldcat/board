(ns board.backend.routes
  (:require
    [board.backend.handlers.dummy :as dummy]
    [board.backend.handlers.echo :as echo]
    [board.backend.handlers.login :as login]
    [board.backend.handlers.register :as register]
    [board.backend.handlers.upload :as upload]
    [board.backend.rate-limit :as limit]
    [muuntaja.core :as muuntaja-core]
    [reitit.coercion :as coercion]
    [reitit.coercion.malli]
    [reitit.coercion.spec :as reitit-coercion-spec]
    [reitit.ring :refer [ring-handler router]]
    [reitit.ring.coercion :as reitit-coercion]
    [reitit.ring.middleware.multipart :refer [multipart-middleware]]
    [reitit.ring.middleware.muuntaja :as wrap-muuntaja]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.params :refer [wrap-params]]))


(def coercion
  {:muuntaja muuntaja-core/instance
   :compile coercion/compile-request-coercers
   :coercion reitit-coercion-spec/coercion
   :middleware [wrap-muuntaja/format-negotiate-middleware
                wrap-muuntaja/format-middleware
                reitit-coercion/coerce-exceptions-middleware
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
           {:post {:handler #'register/register-handler}}]]

         ["/post"
          ["/new"
           {:post {:handler #'upload/upload-handler
                   :middleware [wrap-params
                                wrap-multipart-params]}}]]]

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
