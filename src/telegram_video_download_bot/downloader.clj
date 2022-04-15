(ns telegram-video-download-bot.downloader
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [langohr.consumers :as lc]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [get-mq-connection]]
            [telegram-video-download-bot.telegram :refer [delete-original-message send-chat-action
                                                          send-response-no-match send-video]]
            [telegram-video-download-bot.util :refer [download-file]]))

(defn message-handler
  [_ _  ^bytes payload]
  (log/info "Received message")
  (let [parsed (json/read-str (String. payload "UTF-8"))
        token (get-config-value :token)
        link (get parsed "link")
        chat-id (get parsed "chat-id")
        _ (send-chat-action token chat-id "upload_video")
        message-id (get parsed "message-id")
        reply-to-id (get parsed "reply-to-id" nil)
        file-location (download-file link "/tmp")]
    (if (nil? file-location)
      (send-response-no-match token chat-id message-id (get-config-value :base-error-message))
      (let [error-code (send-video token chat-id file-location reply-to-id)]
        (if (not error-code)
          (do (log/info "File sent succesfully")
              (delete-original-message token chat-id message-id))
          (do
            (log/error "Something went wrong while trying to send file")
            (send-response-no-match token chat-id message-id (str (get-config-value :base-error-message) "(" error-code ")"))))))))

(defn start-downloader []
  (let [{:keys [:ch :qname]} (get-mq-connection)]
    (lc/subscribe ch qname message-handler {:auto-ack true})))