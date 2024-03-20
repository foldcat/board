(ns board.backend.database.posts
  (:require
    [board.backend.database.schema :as schema]
    [board.util.pformat :refer [pformat]]
    [board.util.time :as date]
    [clojure.tools.logging :as log]
    [cryptohash-clj.api :as crypto]
    [datomic.api :as api]))


(defn create-post!
  [{:keys [title description hash extension owner tags]}]
  (api/transact
    (:posts @schema/conn)
    [{:post/title title
      :post/description description
      :post/hash hash
      :post/extension extension
      :post/owner owner
      :post/timestamp (java.util.Date.)
      :post/tags tags}]))


;; TODO limit everything's size
(defn query-all-posts
  []
  (api/q
    '[:find ?e ?title ?description ?hash ?extension
      ?timestamp ?owner ?tags
      :where
      [?e :post/title ?title]
      [?e :post/description ?description]
      [?e :post/hash ?hash]
      [?e :post/extension ?extension]
      [?e :post/timestamp ?timestamp]
      [?e :post/owner ?owner]
      [?e :post/tags ?tags]]
    (api/db (:posts @schema/conn))))


(comment 
  @(create-post! 
     {:title "who"
      :description "first"
      :hash "1234"
      :extension "png"
      :owner 1234
      :tags [["test" "placeholder"]]}))
