(ns board.util.date-validation)


(defn leap-year?
  [year]
  (or
    (and (= (rem year 4) 0)
         (not= (rem year 100) 0))
    (= (rem year 400) 0)))


(defn valid-date?
  [{:keys [year month day]}]
  (let
    [month-lengths
     [31 (if (leap-year? year) 29 28)
      31 30 31 30 31 31 30 31 30 31]]
    (and (<= 1 month 12)
         (<= 1 day
             (nth month-lengths
                  (dec month))))))


(defn not-too-old?
  "no 100+ years old daemons please"
  [m]
  (> (:year m) 1900))
