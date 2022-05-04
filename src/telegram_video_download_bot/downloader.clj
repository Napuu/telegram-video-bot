(ns telegram-video-download-bot.downloader
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [langohr.consumers :as lc]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [global-mq-connection]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.util :refer [download-file]]))

(defn message-handler
  [_ _  ^bytes payload]
  (log/info "Received message")
  (let [parsed (json/read-str (String. payload "UTF-8"))
        token (get-config-value :token)
        link (get parsed "link")
        chat-id (get parsed "chat-id")
        _ (send-telegram-command {:bot-token token :chat-id chat-id :method "sendChatAction" :action "upload_video" })
        message-id (get parsed "message-id")
        reply-to-id (get parsed "reply-to-id" nil)
        file-location (download-file link "/tmp")]
    (if (nil? file-location)
      (send-telegram-command {:bot-token token
                              :chat-id chat-id
                              :method "sendMessage"
                              :reply-to-id message-id
                              :text (get-config-value :base-error-message)})
      (let [error-code (send-telegram-command {:bot-token token
                                               :chat-id chat-id
                                               :method "sendVideo"
                                               :reply-to-id reply-to-id
                                               :file file-location})]
        (if (not error-code)
          (do (log/info "File sent successfully")
              (send-telegram-command {:bot-token token
                                      :chat-id chat-id
                                      :method "deleteMessage"
                                      :deleted-message-id message-id}))
          (do
            (log/error "Something went wrong while trying to send file")
            (send-telegram-command {:bot-token token
                                    :chat-id chat-id
                                    :method "sendMessage"
                                    :reply-to-id reply-to-id
                                    :text (str (get-config-value :base-error-message) "(" error-code ")")})))))))

(defn start-downloader []
  (let [{:keys [:ch :qname]} @@global-mq-connection]
    (lc/subscribe ch qname message-handler {:auto-ack true})
    (log/info "Downloader up and running")))