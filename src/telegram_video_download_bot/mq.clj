(ns telegram-video-download-bot.mq
  (:require [clojure.data.json :as json]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]))

(def server1-conn {:pool {} :spec {:uri "http://debian-general2:6379"}})

(def LINK_QUEUE "video-download-bot.link-queue")

(def ^{:const true}
  default-exchange-name "")

(defn get-rmq-connection
  "init rmq connection, return [ch qname]" []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "video-download-bot.link-queue"]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (lq/declare ch qname {:exclusive false :auto-delete true})
    {:ch ch :qname qname}))

(def conn (get-rmq-connection))

(defn enqueue-link
  "Send link to message queue
     link - link to the actual video
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
  [& {:keys [link chat-id message-id reply-to-id]}]
  (lb/publish (conn :ch) default-exchange-name (conn :qname)
              (json/write-str {:link link :chat-id chat-id :message-id message-id :reply-to-id reply-to-id})
              {:content-type "text/plain" :type "telegram.link"}))
