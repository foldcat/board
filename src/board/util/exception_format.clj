(ns board.util.exception-format)

(defn map->invalid-req
  [body]
  {:status 400
   :body body})

