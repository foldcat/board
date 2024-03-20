(ns board.backend.rate-limit
  (:require
    [board.util.pformat :refer [pformat]]
    [clojure.tools.logging :as log]
    [farolero.extensions.flow]
    [fmnoise.flow :as flow]))


(def ip-pool
  {:anonymous (atom {})
   :known (atom {})})


(def user-pool
  (atom {}))


;; when user connect 
;; if not already, log the user ip into rate limit pool 
;; check if rate is 0 for the ip, if yes, return error 
;; if rate limit is not reached, decrement the remaining 
;; refresh rate limit every 10 seconds


;; stop worrying about a fast implementation
;; its not like the service will get a lot of visits anyways
;; - f40

(defn insert-known!
  [ip id]
  (swap! user-pool
         conj [id 20])
  (swap! (:known ip-pool)
         conj [ip 20]))


(defn insert-unknown!
  [ip]
  (swap! (:known ip-pool)
         conj [ip 10]))


(defn insert!
  [ip id])


(defmulti wrap-limit
  (fn [request]
    (:request-method request)))


#_(defmethod wrap-limit :get
    [request]
    ())


(defmethod wrap-limit :default
  [request]
  nil)
