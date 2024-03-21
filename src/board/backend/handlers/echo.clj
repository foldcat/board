(ns board.backend.handlers.echo
  (:require
    [board.util.pformat :refer [pformat]]
    [taoensso.timbre :as log]))


(defn echo
  [request]
  (log/info "echo received!")
  (log/info (pformat request))
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (pformat request)})


