(ns telegram-video-download-bot.downloader
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [langohr.consumers :as lc]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [global-mq-connection]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.util :refer [download-file]]))

(comment
  (let [json-string "{\"kissa\": 123}"]
    (:kissa (json/read-str json-string :key-fn keyword))))

(defn handle-successful-download
  [token chat-id reply-to-id file-location message-id base-error-message]
  (let [status-code (send-telegram-command {:bot-token   token
                                            :chat-id     chat-id
                                            :method      "sendVideo"
                                            :reply-to-id reply-to-id
                                            :file        file-location})]
    (if (= status-code 200)
      (do (log/info "File sent successfully")
          (send-telegram-command {:bot-token          token
                                  :chat-id            chat-id
                                  :method             "deleteMessage"
                                  :deleted-message-id message-id}))
      (do
        (log/error "Something went wrong while trying to send file")
        (send-telegram-command {:bot-token   token
                                :chat-id     chat-id
                                :method      "sendMessage"
                                :reply-to-id reply-to-id
                                :text        (str base-error-message " (" status-code ")")})))))

(defn handle-unsuccessful-download
  [token chat-id message-id base-error-message]
  (send-telegram-command {:bot-token   token
                          :chat-id     chat-id
                          :method      "sendMessage"
                          :reply-to-id message-id
                          :text        base-error-message}))

(defn message-handler
  [_ _ ^bytes payload]
  (log/info "Received message")
  (let [{link :link
         chat-id :chat-id
         message-id :message-id
         reply-to-id :reply-to-id} (json/read-str (String. payload "UTF-8") :key-fn keyword)
        token (get-config-value :token)
        base-error-message (get-config-value :base-error-message)]

    (log/info "Starting file download")
    (send-telegram-command {:bot-token token :chat-id chat-id :method "sendChatAction" :action "upload_video"})

    (let [file-location (download-file link "/tmp")]
      (if (nil? file-location)
        (handle-unsuccessful-download token chat-id message-id base-error-message)
        (handle-successful-download token chat-id reply-to-id file-location message-id base-error-message)))))

(defn start-downloader []
  (let [{:keys [:ch :qname]} @@global-mq-connection]
    (lc/subscribe ch qname message-handler {:auto-ack true})
    (log/info "Downloader up and running")))