(ns board.backend.core
  (:require
    [board.backend.routes :as routes]
    [clojure.tools.logging :as log]
    [org.httpkit.server :refer [run-server]]))


(defonce server
  (atom nil))


(defn start-server!
  []
  (if @server
    (log/warn "server already started")
    (do
      (reset!
        server
        (run-server
          #'routes/app
          {:worker-pool
           (java.util.concurrent.Executors/newVirtualThreadPerTaskExecutor)
           :port 8080}))
      (log/info "started server!"))))


(defn stop-server!
  []
  (if @server
    (do
      (@server 1000)
      (reset! server nil)
      (log/info "stopping server!"))
    (log/warn "server is not started yet")))


(defn restart-server
  []
  (stop-server!)
  (start-server!))


(comment 
  (stop-server!)
  (start-server!)
  (restart-server))
