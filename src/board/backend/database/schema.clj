(ns board.backend.database.schema
  "controls the database schema"
  (:require
    [clojure.tools.logging :as log]
    [datomic.api :as api]))


(defonce conn
  (atom
    {:posts nil
     :users nil}))


(defn void
  "voids return of functions
  used to suppress linter warnings"
  [& _]
  nil)


(defn db-uri-base
  "function to generate database uri"
  [uri user password db-name]
  (str "datomic:sql://"
       db-name
       "?jdbc:postgresql://"
       uri
       "/datomic?user="
       user
       "&password="
       password))


(def db-uri-builder
  "partially filled database uri"
  (partial db-uri-base
           "localhost:5432"
           "datomic"
           "datomic"))


(def posts (db-uri-builder "posts"))
(def users (db-uri-builder "users"))


(def all-uri #{posts users})


(defn connection-up!
  [nm db-uri]
  (swap!
    conn
    #(assoc
       % nm
       (api/connect
         db-uri)))
  (log/info
    "The db connection" nm "should be up!"))


(defn make-db!
  [db-uri]
  (api/create-database db-uri)
  (log/info
    "The database" db-uri "should be created!"))


(defn destroy-db!
  [db-uri]
  (api/delete-database db-uri)
  (log/info
    "The database" db-uri "should be dropped."))


(defn connect-all!
  []
  (connection-up! :users users)
  (connection-up! :posts posts))


(defn db-up!
  []
  (void (pmap make-db! all-uri))
  (connect-all!)

  @(api/transact
     (:posts @conn)
     [{:db/doc "stores the data of posts"}])
  @(api/transact
     (:users @conn)
     [{:db/doc "stores the data of users"}])


  @(api/transact
     (:posts @conn)

     [{:db/ident :post/title
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "the title of the post"}

      {:db/ident :post/description
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "the description of the post"}

      {:db/ident :post/hash
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/doc "the hash of the post image, 
       doubles as it's the file name of it"}

      {:db/ident :post/extension
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "the file extension of the image"}

      {:db/ident :post/timestamp
       :db/valueType :db.type/instant
       :db/cardinality :db.cardinality/many
       :db/doc "the time where the post is posted"}

      {:db/ident :post/owner
       :db/valueType :db.type/long
       :db/cardinality :db.cardinality/many
       :db/doc "the id of the original poster"}

      {:db/ident :post/tags
       :db/valueType :db.type/tuple
       :db/tupleType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "the tags attached the post"}])

  @(api/transact
     (:users @conn)

     [{:db/ident :users/username
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "username of the user"}

      {:db/ident :users/identifier
       :db/valueType :db.type/long
       :db/cardinality :db.cardinality/one
       :db/doc "identitier of the user as number"}

      {:db/ident :users/display-name
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "display name of the user"}

      {:db/ident :users/email
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/doc "email of the user"}

      {:db/ident :users/join-time
       :db/valueType :db.type/instant
       :db/cardinality :db.cardinality/one
       :db/doc "moment the user successfully register"}

      {:db/ident :users/verified?
       :db/valueType :db.type/boolean
       :db/cardinality :db.cardinality/one
       :db/doc "is the user email verified"}

      {:db/ident :users/password
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "hashed password"}

      {:db/ident :users/date-of-birth
       :db/valueType :db.type/instant
       :db/cardinality :db.cardinality/one
       :db/doc "hashed password"}])

  (log/info
    "Created database schema!"))


(defn drop-all-db!
  []
  (pmap destroy-db! all-uri))


(comment 
  (db-up!)
  (connect-all!)
  (drop-all-db!))
