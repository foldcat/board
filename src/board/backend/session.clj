(ns board.backend.session
  (:require
    [board.util.vthread :refer [vthread]]
    [farolero.core :as far]
    [java-time.api :as jt])
  (:import
    (java.security
      SecureRandom)
    (java.util
      Base64)))


(defrecord session
  [token ip local-date-time auto-logout? owner])


(def keystore (ref []))
(def amount (ref 0))


(defn generate-random-string!
  [length]
  (let [random-bytes (byte-array length)
        secure-random (SecureRandom.)
        encoder (.withoutPadding (Base64/getEncoder))]
    (.nextBytes secure-random random-bytes)
    (.encodeToString encoder random-bytes)))


(defn make-session*
  [date ip auto-logout? owner]
  (dosync
    (alter amount inc)
    (let [token
          (str
            @amount
            (generate-random-string! 32))]
      (alter
        keystore
        #(conj
           %
           (->session
             token
             ip
             date
             auto-logout?
             owner)))
      token)))


(def make-session!
  (partial make-session* (jt/local-date-time)))


(defn get-session-
  [token]
  (first (filter #(= token (:token %)) @keystore)))


(defn tomorrow
  [local-date-time]
  (jt/plus
    local-date-time
    (jt/days 1)))


(defn expired?
  [token]
  (let
    [target (get-session- token)]
    (and (:auto-logout? target)
         (jt/after?
           (jt/local-date-time)
           (tomorrow (:local-date-time target))))))


(defn sweep-keystore
  "deletes 1 day old idle session"
  [store]
  (let [current (jt/local-date-time)]
    (vec
      (filter
        (fn [{:keys [local-date-time auto-logout?]}]
          (not
            (and auto-logout?
                 (jt/after?
                   current
                   (tomorrow local-date-time)))))
        store))))


(defn start-sweeper!
  "removes 1 day old sessoon every 15 minutes"
  []
  (vthread
    (dosync
      (alter keystore sweep-keystore))
    (Thread/sleep
      (java.time.Duration/ofMinutes 15))))


(defn update-session!
  "updates a session's local date time to right now"
  [token]
  (dosync
    (if-let [target (first
                      (filter
                        #(= token (:token %))
                        @keystore))]
      (alter keystore
             (fn [store]
               (-> (filter #(not= target %) store)
                   (conj
                     (assoc target :local-date-time
                            (jt/local-date-time)))
                   vec)))
      (far/error ::unknown-session))))


(defn void-session!
  "deletes session"
  [token]
  (dosync
    (alter
      keystore
      (fn [store]
        (vec (filter #(= token (:token %)) store))))))


(defn valid-session?
  "is session valid, if expired delete"
  [token]
  (if-let [session (get-session- token)]
    (if (expired? session)
      (do (void-session! token) true)
      true)
    false))


(comment
  (def expired
   (make-session*
     (jt/local-date-time 2001 1 1)
     "192.168.1.1"
     true
     "123"))
  (get-session- expired)
  (expired? expired)
  (make-session! "192.168.2.1" true "123")
  (def token (make-session! "192.168.2.1" false "123"))
  (void-session! token)
  (update-session! token)
  (println @keystore)
  @keystore
  (start-sweeper!))
