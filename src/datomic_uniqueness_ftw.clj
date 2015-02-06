(ns datomic-uniqueness-ftw
  (require [datomic.api :as d]))

(def db-url "datomic:mem://datomic-uniqueness-ftw-snowflakes")
(d/delete-database db-url)
(d/create-database db-url)
(def db-conn (d/connect db-url))

(def schema
  [{:db/ident :snowflake/shape,
    :db/unique :db.unique/identity,
    :db/valueType :db.type/string,
    :db.install/_attribute :db.part/db,
    :db/cardinality :db.cardinality/one,
    :db/doc "image URL",
    :db/id #db/id[:db.part/db]}
   {:db/ident :snowflake/mass,
    :db/valueType :db.type/long,
    :db.install/_attribute :db.part/db,
    :db/cardinality :db.cardinality/one,
    :db/id #db/id[:db.part/db]}
   {:db/ident :snowflake/color,
    :db/valueType :db.type/string,
    :db.install/_attribute :db.part/db,
    :db/cardinality :db.cardinality/one,
    :db/id #db/id[:db.part/db]}])

(d/transact db-conn schema)

(defn step1
  "Assert something."
  []
  (println @(d/transact db-conn
                        [{:db/id (d/tempid :db.part/user)
                          :snowflake/shape "id-1"
                          :snowflake/mass 17
                          :snowflake/color "blue"}]))

  (d/touch (d/entity (d/db db-conn) [:snowflake/shape "id-1"])))

(defn step2
  "Demonstrates use of a lookup ref as a :db/id."
  []
  (println @(d/transact db-conn
                        [{:db/id [:snowflake/shape "id-1"]
                          :snowflake/mass 5000
                          :snowflake/color "green"}]))

  (d/touch (d/entity (d/db db-conn) [:snowflake/shape "id-1"])))

(defn step3
  "This demonstrates what I think is being described in the following
  two places in the documentation.

  Place 1: http://docs.datomic.com/tutorial.html
    \"Temporary ids are mapped to new entity ids unless you use a
      temporary id with an attribute defined as :db/unique
      :db.unique/identity, in which case the system will map your
      temporary id to an existing entity if one exists with the same
      attribute and value (update) ... All further adds in the
      transaction that apply to that same temporary id are applied to
      the \"upserted\" entity.\"

  Place 2: http://docs.datomic.com/identity.html
    \"If a transaction specifies a unique identity for a temporary id,
      and that unique identity already exists in the database, then
      that temporary id will resolve to the existing entity in the
      system. \"

  I think Place 2 is unclear, and would be better if it said something
  like:
    \"If a transaction specifies a temporary id for an entity along
      with a value for a :db.unique/identity attribute of that entity,
      and that unique identity value already exists for an entity in
      the database, then that temporary id will resolve to the
      existing entity in the system. \"
  "
  []
  (println @(d/transact db-conn
                        [{:db/id (d/tempid :db.part/user -1)
                          ;; This forces the tempid to resolve to the
                          ;; existing entity ID.
                          :snowflake/shape "id-1"}
                         {:db/id (d/tempid :db.part/user -1)
                          ;; Now we can use that tempid to assert
                          ;; other attributes, or whatever.
                          :snowflake/color "orange"}]))

  (d/touch (d/entity (d/db db-conn) [:snowflake/shape "id-1"])))

