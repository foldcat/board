(ns board.backend.database.users
  (:require
    [board.backend.database.schema :as schema]
    [board.util.pformat :refer [pformat]]
    [board.util.time :as date]
    [clojure.tools.logging :as log]
    [cryptohash-clj.api :as crypto]
    [datomic.api :as api]))


(defn query-*
  []
  (api/q
    '[:find ?e ?username ?identifier ?dob ?email
      ?password ?join-time
      :where
      [?e :users/username ?username]
      [?e :users/identifier ?identifier]
      [?e :users/password ?password]
      [?e :users/join-time ?join-time]
      [?e :users/date-of-birth ?dob]
      [?e :users/email ?email]]
    (api/db (:users @schema/conn))))


(defn query-entity
  []
  (api/q
    '[:find ?e
      :where [?e :users/username]]
    (api/db (:users @schema/conn))))


(defn query-id
  "query id from email"
  [email]
  (api/q
    '[:find ?e
      :in $ ?email
      :where [?e :users/email ?email]]
    (api/db (:users @schema/conn)) email))


(defn query-password
  [email]
  (api/q
    '[:find ?password
      :in $ ?email
      :where [?e :users/email ?email]
      [?e :users/password ?password]]
    (api/db (:users @schema/conn)) email))


(defn query-largest-identifier
  [username]
  (api/q
    '[:find (max ?identifier)
      :in $ ?username
      :where
      [?e :users/username ?username]
      [?e :users/identifier ?identifier]]
    (api/db (:users @schema/conn)) username))


(defn query-email
  [email]
  (api/q
    '[:find ?e
      :in $ ?email
      :where [?e
              :users/email ?email]]
    (api/db (:users @schema/conn)) email))


(defn gen-identifier
  [username]
  (log/info "generating identifier for" username)
  (if-let [existance
           (-> username
               query-largest-identifier
               seq
               ffirst)]
    (inc existance)
    1))


(defn register-user!
  [{:keys [username email password date-of-birth]}]
  (api/transact
    (:users @schema/conn)

    [{:users/username username

      :users/email email

      :users/password
      (crypto/hash-with :argon2 password)

      :users/join-time (java.util.Date.)

      :users/verified? false

      :users/identifier
      (gen-identifier username)

      :users/display-name username

      :users/date-of-birth
      (date/map->date date-of-birth)}]))


(comment 
  @(register-user! 
     {:username "who" 
      :email "who@who.com" 
      :password "asdf" 
      :date-of-birth 
      {:year 1990 
       :month 1 
       :day 1}}) 
  @(create-post! 
     {:title "who"
      :description "first"
      :hash "1234"
      :extension "png"
      :owner 1234
      :tags [["test" "placeholder"]]})
  (query-password "dfjsss@ver.com")
  (query-id "who")
  (query-largest-identifier "who")
  (gen-identifier "who")
  (count (query-*))
  (query-all-posts)
  (log/info (pformat (query-*)))
  (query-email "who@who.com"))
