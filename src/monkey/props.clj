(ns monkey.props
  "This namespace holds all of the logic for managing configuration values."
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [java.net URL]
           [clojure.lang PersistentArrayMap]))


(def ^{:private true :const true} default-prop-values
  {"monkey.amqp.uri"                  "amqp://guest:guestPW@localhost:5672"
   "monkey.amqp.queue"                "monkey"
   "monkey.amqp.exchange.name"        "de"
   "monkey.amqp.exchange.durable"     "True"
   "monkey.amqp.exchange.auto-delete" "False"
   "monkey.es.url"                    "http://elasticsearch:9200"
   "monkey.es.index"                  "data"
   "monkey.es.tag-type"               "tag"
   "monkey.es.batch-size"             "1000"
   "monkey.es.scroll-size"            "1000"
   "monkey.es.scroll-timeout"         "1m"
   "monkey.log-progress-enabled"      "False"
   "monkey.log-progress-interval"     "10000"
   "monkey.retry-period-ms"           "1000"
   "monkey.tags.host"                 "dedb"
   "monkey.tags.port"                 "5432"
   "monkey.tags.db"                   "metadata"
   "monkey.tags.user"                 "de"
   "monkey.tags.password"             "notprod"
   "monkey.tags.batch-size"           "10"})


(def ^{:private true :const true} prop-names
  (set (keys default-prop-values)))


(defn- ^String get-prop
  "Retrieves a string configuration setting.

   Parameters:
     props     - the property map
     prop-name - the name of the property

   Returns:
     the property value"
  [^PersistentArrayMap props ^String prop-name]
  (str (or (get props prop-name)
           (get default-prop-values prop-name))))


(defn ^String amqp-uri
  "Returns the URI used to connect to the AMQP broker.

   Parameters:
     props - the property map to use

   Returns:
     the AMQP broker uri"
  [^PersistentArrayMap props]
  (string/trim (get-prop props "monkey.amqp.uri")))

(defn ^String amqp-exchange-name
  "Returns the name AMQP exchange respondsible for routing messages to monkey.

   Parameters:
     props - the property map to use

   Returns:
     tue AMQP exchange name"
  [^PersistentArrayMap props]
  (get-prop props "monkey.amqp.exchange.name"))


(defn ^Boolean amqp-exchange-durable?
  "Indicates whether or not the exchange is durable.

   Parameters:
     props - the property map to use

   Returns:
     true if the exchange is durable, otherwise false"
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get-prop props "monkey.amqp.exchange.durable"))))


(defn ^Boolean amqp-exchange-auto-delete?
  "Indicates whether or not broker auto delete's this exchange.

   Parameters:
     props - the property map to use

   Returns:
     true if the exchange is automatically deleted, otherwise false"
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get-prop props "monkey.amqp.exchange.auto-delete"))))


(defn ^String amqp-queue
  "Returns the AMQP queue name.

   Parameters:
     props - the property map to use

   Returns:
     the queue name"
  [^PersistentArrayMap props]
  (get-prop props "monkey.amqp.queue"))


(defn ^Integer es-batch-size
  "Returns the indexing bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of documents to handle at once in a bulk indexing operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.es.batch-size"))))


(defn ^URL es-url
  "Returns the elasticsearch base URL.

   Parameters:
     props - The property map to use.

   Returns:
     It returns the elasticsearch base URL."
  [^PersistentArrayMap props]
  (URL. (string/trim (get-prop props "monkey.es.url"))))

(defn ^String es-user
  "Returns the user for authentication with elasticsearch.

   Parameters:
     props - the property map to use

   Returns:
     the elasticsearch username"
  [^PersistentArrayMap props]
  (get-prop props "monkey.es.username"))

(defn ^String es-password
  "Returns the password for authentication with elasticsearch.

   Parameters:
     props - the property map to use

   Returns:
     the elasticsearch password"
  [^PersistentArrayMap props]
  (get-prop props "monkey.es.password"))


(defn ^String es-index
  "Returns the index in elasticsearch where the tags are indexed.

   Parameters:
     props - the property map to use

   Returns:
     the name of the index"
  [^PersistentArrayMap props]
  (get-prop props "monkey.es.index"))


(defn ^String es-tag-type
  "returns the elasticsearch mapping type for a tag

   Parameters:
     props - the property map to use

   Returns:
     the tag mapping type"
  [^PersistentArrayMap props]
  (get-prop props "monkey.es.tag-type"))


(defn ^Integer es-scroll-size
  "Returns the number of documents to retrieve at a time when scrolling through an elasticsearch
   result set.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll size"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.es.scroll-size"))))


(defn ^String es-scroll-timeout
  "Returns the unitted timeout value for a scroll.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll timeout"
  [^PersistentArrayMap props]
  (string/trim (get-prop props "monkey.es.scroll-timeout")))


(defn ^Boolean log-progress?
  "Indicates whether or not progress logging is enabled.

   Parameters:
     props - the property map to use

   Returns:
     It returns the true if progress should be logged, otherwise false."
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get-prop props "monkey.log-progress-enabled"))))


(defn ^Integer progress-logging-interval
  "It returns the number of items that must of been processed before progress is logged.

   Parameters:
     props - the property map to use

   Returns:
     It returns the item count."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.log-progress-interval"))))


(defn ^Integer retry-period
  "It returns the amount of time to wait in milliseconds before retrying an operation.

   Parameters:
     props - the property map to use

   Returns:
     It return the period to wait."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.retry-period-ms"))))


(defn ^String tags-host
  "Returns the tags database host

   Parameters:
     props - The property map to use.

   Returns:
     It returns the domain name of the host of the tags database."
  [^PersistentArrayMap props]
  (string/trim (get-prop props "monkey.tags.host")))


(defn ^Integer tags-port
  "Returns the tags database port

   Parameters:
     props - The property map to use.

   Returns:
     It returns the port the tags database listens on."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.tags.port"))))


(defn ^String tags-db
  "Returns the name of the tags database

   Parameters:
     props - the property map to use

   Returns:
     It returns the name of the tags database"
  [^PersistentArrayMap props]
  (get-prop props "monkey.tags.db"))


(defn ^String tags-user
  "Returns the username authorized to access the tags database.

   Parameters:
     props - The properties map to use.

   Returns:
     It returns the authorized username."
   [^PersistentArrayMap props]
  (get-prop props "monkey.tags.user"))


(defn ^String tags-password
  "returns the password used to authenticate the authorized user

   Parameters:
     props - the property map to use

   Returns:
     It returns the password"
  [^PersistentArrayMap props]
  (get-prop props "monkey.tags.password"))


(defn ^Integer tags-batch-size
  "Returns the tags inspection bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of tags to handle at once in a bulk inspection operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get-prop props "monkey.tags.batch-size"))))
