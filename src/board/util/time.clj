(ns board.util.time
  (:require
    [java-time.api :as jt]))


(defn map->date
  [{:keys [year month day]}]
  (jt/java-date
    (jt/with-zone
      (jt/zoned-date-time year month day)
      "Hongkong")))


(defn ->local-date
  [instant]
  (.toLocalDate
    (.atZone
      (.toInstant instant)
      (jt/zone-id "Hongkong"))))
