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
  (let [_conn  (rmq/connect)
        _ch    (lch/open _conn)
        _qname "video-download-bot.link-queue"]
    (lq/declare _ch _qname {:exclusive false :auto-delete true})
    {:ch _ch :qname _qname :conn _conn}))

(defn enqueue-link
  "Send link to message queue
     link - link to the actual video
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
  [& {:keys [link chat-id message-id reply-to-id]}]
  (log/info "Sending message to queue")
  ; Fairly sure that getting the connection every time this way is very bad,
  ; but with this amount of users it's not a big deal.
  (let [{:keys [ch qname conn]} (get-rmq-connection)]
    (lb/publish ch default-exchange-name qname
                (json/write-str {:link link :chat-id chat-id :message-id message-id :reply-to-id reply-to-id})
                {:content-type "text/plain" :type "telegram.link"})
    (rmq/close ch)
    (rmq/close conn)))
