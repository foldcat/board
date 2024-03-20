(ns board.backend.handlers.dummy)


(defn dummy-handler
  [_request]
  {:status 200
   :body {:who true
          :asked true}})


