(ns board.util.vthread
  (:import
    (java.util.concurrent
      Executors)))


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


(set-agent-send-executor!
  (Executors/newThreadPerTaskExecutor
    (thread-factory "clojure-agent-send-pool-")))
