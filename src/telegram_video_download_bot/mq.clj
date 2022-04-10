(ns telegram-video-download-bot.mq
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]))

(def ^{:const true}
  default-exchange-name "")

(defn get-rmq-connection
  "init rmq connection, return [ch qname]" []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "video-download-bot.link-queue"]
    (log/info "Connected to RabbitMQ")
    (lq/declare ch qname {:exclusive false :auto-delete true})
    {:ch ch :qname qname}))

(defn enqueue-link
  "Send link to message queue
     link - link to the actual video
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
  [& {:keys [link chat-id message-id reply-to-id conn]}]
  (log/info "Sending message to queue")
  (lb/publish (conn :ch) default-exchange-name (conn :qname)
              (json/write-str {:link link :chat-id chat-id :message-id message-id :reply-to-id reply-to-id})
              {:content-type "text/plain" :type "telegram.link"}))
