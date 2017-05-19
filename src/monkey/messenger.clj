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
  (:import [clojure.lang IFn PersistentArrayMap]
           [org.cyverse.events.ping PingMessages$Pong]
           [com.google.protobuf.util JsonFormat]))


;; TODO redesign so that the connection logic becomes testable


(defn- attempt-connect
  [props]
  (try
    (let [conn (amqp/connect {:uri (props/amqp-uri props)})]
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
    (exchange/topic ch exchange
      {:durable     (props/amqp-exchange-durable? props)
       :auto-delete (props/amqp-exchange-auto-delete? props)})
    (queue/declare ch queue {:durable true})
    (doseq [key ["index.all" "index.tags" "events.monkey.#"]]
      (queue/bind ch queue exchange {:routing-key key}))
    queue))


(defn- handle-delivery
  [_ deliver ch metadata _]
  (let [delivery-tag (:delivery-tag metadata)]
    (try
      (log/info "received reindex tags message")
      (deliver)
      (basic/ack ch delivery-tag)
      (catch Throwable t
        (log/error t "metadata reindexing failed, rescheduling")
        (basic/reject ch delivery-tag true)))))


(defn- handle-ping
  [props _ channel {:keys [delivery-tag routing-key] :as metadata} msg]
  (basic/ack channel delivery-tag)
  (log/info (format "[messenger/handle-ping] [%s] [%s]" routing-key (String. msg)))
  (basic/publish channel (props/amqp-exchange-name props) "events.monkey.pong"
    (.print (JsonFormat/printer)
      (.. (PingMessages$Pong/newBuilder)
        (setPongFrom "monkey")
        (build)))))


(def handlers
  {"index.all"          handle-delivery
   "index.tags"         handle-delivery
   "events.monkey.ping" handle-ping})


(defn route-messages
  [props deliver channel {:keys [routing-key] :as metadata} msg]
  (let [handler (get handlers routing-key)]
    (if-not (nil? handler)
      (handler props deliver channel metadata msg)
      (log/error (format "[events/route-messages] [%s] [%s] no handler" routing-key (String. msg))))))


(defn- receive
  [conn props notify-received]
  (let [ch       (ch/open conn)
        queue    (prepare-queue ch props)]
    (log/info (format "Created AMQP reindexing queue. queue=%s" queue))
    (consumer/blocking-subscribe ch queue (partial route-messages props notify-received))))


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
