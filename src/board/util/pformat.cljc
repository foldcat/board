(ns board.util.pformat
  (:require
    [clojure.pprint :refer [pprint]]))


(defn pformat
  "formats strings pprint style"
  [& args]
  (with-out-str
    (apply pprint args)))
