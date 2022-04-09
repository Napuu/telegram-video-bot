(ns telegram-video-download-bot.downloader
  (:require [clojure.data.json :as json]
            [environ.core :refer [env]]
            [langohr.consumers :as lc]
            [telegram-video-download-bot.mq :refer [get-rmq-connection]]
            [telegram-video-download-bot.telegram :refer [delete-original-message
                                                          send-response-no-match send-video]]
            [telegram-video-download-bot.util :refer [download-file]]))

; TODO - move this to core.clj
(def LINK_QUEUE "video-download-bot.link-queue")

(defn parse-link-from-queue
  "Parses link that was received from
     message queue"
  [{:keys [link chat-id reply-to-id]}]
  (when (not link)
    (println "No 'link' provided"))
  (println "Hello from handler" link chat-id reply-to-id))

(def token (env :telegram-token))

(defn message-handler
  [_ _  ^bytes payload]
  (println (String. payload "UTF-8"))
  (let [parsed (json/read-str (String. payload "UTF-8"))
        link (get parsed "link")
        chat-id (get parsed "chat-id")
        message-id (get parsed "message-id")
        reply-to-id (get parsed "reply-to-id" nil)
        file-location (download-file link "/tmp")]
    (if (nil? file-location)
      (send-response-no-match token chat-id message-id)
      ((println "success")
       (if (send-video token chat-id file-location reply-to-id)
         ((println "success at send")
          (delete-original-message token chat-id message-id))
         (println "something went wrong while trying to send file"))
       ))))


(defn start-downloader []
  (let [[ch qname] (get-rmq-connection)]
    (println "ch, qname:" ch qname)
    (lc/subscribe ch qname message-handler {:auto-ack true})))