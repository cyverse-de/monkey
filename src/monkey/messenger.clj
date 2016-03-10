(ns monkey.messenger
  "This namespace implements the Messages protocol where langhor is used to interface with an AMQP
   broker."
  (:require [clojure.tools.logging :as log]
            [langohr.basic :as basic]
            [langohr.channel :as ch]
            [langohr.consumers :as consumer]
            [langohr.core :as amqp]
            [langohr.exchange :as exchange]
            [langohr.queue :as queue]
            [monkey.props :as props])
  (:import [clojure.lang IFn PersistentArrayMap]))


;; TODO redesign so that the connection logic becomes testable


(defn- attempt-connect
  [props]
  (try
    (let [conn (amqp/connect {:host     (props/amqp-host props)
                              :port     (props/amqp-port props)
                              :username (props/amqp-user props)
                              :password (props/amqp-password props)})]
      (log/info "successfully connected to AMQP broker")
      conn)
    (catch Throwable t
      (log/error t "failed to connect to AMQP broker"))))


(defn- connect
  [props]
  (if-let [conn (attempt-connect props)]
    conn
    (do
      (Thread/sleep (props/retry-period props))
      (recur props))))


(defn- prepare-queue
  [ch props]
  (let [exchange (props/amqp-exchange-name props)
        queue    (props/amqp-queue props)]
    (exchange/direct ch exchange
      {:durable     (props/amqp-exchange-durable? props)
       :auto-delete (props/amqp-exchange-auto-delete? props)})
    (queue/declare ch queue {:durable true})
    (doseq [key ["index.all" "index.tags"]]
      (queue/bind ch queue exchange {:routing-key key}))
    queue))


(defn- handle-delivery
  [deliver ch metadata _]
  (let [delivery-tag (:delivery-tag metadata)]
    (try
      (log/info "received reindex tags message")
      (deliver)
      (basic/ack ch delivery-tag)
      (catch Throwable t
        (log/error t "metadata reindexing failed, rescheduling")
        (basic/reject ch delivery-tag true)))))


(defn- receive
  [conn props notify-received]
  (let [ch       (ch/open conn)
        queue    (prepare-queue ch props)]
    (consumer/blocking-subscribe ch queue (partial handle-delivery notify-received))))


(defn- silently-close
  [conn]
  (try
    (amqp/close conn)
    (catch Throwable _)))


(defn listen
  "This function monitors an AMQP exchange for tags reindexing messages. When it receives a message,
   it calls the provided function to trigger a reindexing. It never returns.

    Parameters:
      props           - the configuration properties
      notify-received - the function to call when a message is received"
  [^PersistentArrayMap props ^IFn notify-received]
  (let [conn (connect props)]
    (try
      (receive conn props notify-received)
      (catch Throwable t
        (log/error t "reconnecting to AMQP broker"))
      (finally
        (silently-close conn))))
  (Thread/sleep (props/retry-period props))
  (recur props notify-received))
