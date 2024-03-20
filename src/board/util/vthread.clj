(ns board.util.vthread)


(defmacro vthread
  [& expr]
  `(.start
     (Thread/ofVirtual)
     (reify Runnable
       (run
         [this#]
         ~@expr))))


(defn thread-factory
  [name]
  (-> (Thread/ofVirtual)
      (.name name 0)
      (.factory)))
