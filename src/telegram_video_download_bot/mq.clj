(ns telegram-video-download-bot.mq
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [telegram-video-download-bot.config :refer [get-config-value]]))

(def ^{:const true}
  default-exchange-name "")

(defn get-mq-connection
  "Init rmq connection, return [ch qname]" []
  (log/info "Trying to acquire MQ connection")
  (try (let [conn  (rmq/connect {:host (get-config-value :mq-host)
                                 :port (get-config-value :mq-port)})
             ch    (lch/open conn)
             qname "video-download-bot.link-queue"]
         (lq/declare ch qname {:exclusive false :auto-delete true})
         (log/info "MQ connection acquired")
         {:ch ch :qname qname})
       (catch Exception _
         (log/warn "Could not connect to the message queue. Waiting for a while before trying again...")
         (Thread/sleep 5000)
         (get-mq-connection))))

(def global-mq-connection (atom (delay (get-mq-connection))))

(defn _enqueue-link
  [& {:keys [link chat-id start duration message-id reply-to-id connection]}]
  (log/info "Sending message to queue")
  (let [{:keys [ch qname]} connection]
    (lb/publish ch default-exchange-name qname
                (json/write-str {:link link :chat-id chat-id :start start :duration duration :message-id message-id :reply-to-id reply-to-id})
                {:content-type "text/plain" :type "telegram.link"})))

(defn enqueue-link
  "Send link to message queue - retries on failure
     link - link to the actual video
     start - trim video start in seconds
     duration - duration of trimmed clip in seconds
     message-id - id of the original message
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
  [& {:keys [link chat-id start duration message-id reply-to-id retries]}]
  (let [connection @global-mq-connection
        retries (or retries 0)]
    (if (< retries 10)
      (try
        (_enqueue-link
          :connection @connection :link link
          :start start :duration duration
          :chat-id chat-id :message-id message-id :reply-to-id reply-to-id)
        (catch Exception _
          (log/error "Mq connection is not ok, retrying...")
          (Thread/sleep 2000)
          (enqueue-link
           :retries (inc retries)
           :link link
           :start start
           :duration duration
           :chat-id chat-id
           :message-id message-id
           :reply-to-id reply-to-id)))
      (log/warn "Too many retries, giving up..."))))
