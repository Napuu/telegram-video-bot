(ns telegram-video-download-bot.mq
  (:require [clojure.data.json :as json]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [taoensso.carmine :as car]
            [taoensso.carmine.message-queue :as car-mq]))

(def server1-conn {:pool {} :spec {:uri "http://debian-general2:6379"}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

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
    ;(lc/subscribe ch qname message-handler {:auto-ack true})
    ;(lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
    [ch qname]))

(def conn (get-rmq-connection))

(defn enqueue-link
  "Send link to message queue
     link - link to the actual video
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
  [& {:keys [link chat-id message-id reply-to-id]}]
  (println "link, chat-id, reply-to-id:" link chat-id reply-to-id)
  (lb/publish (first conn) default-exchange-name (last conn)
              (json/write-str {:link link :chat-id chat-id :message-id message-id :reply-to-id reply-to-id})
              {:content-type "text/plain" :type "telegram.link"}))
