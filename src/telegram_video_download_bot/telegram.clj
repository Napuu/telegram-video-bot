(ns telegram-video-download-bot.telegram
  (:require [telegram-video-download-bot.config :refer [get-config-value]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]))

(defn send-telegram-command
  "Send command to Telegram api. Takes dict as a parameter.
  Return status code.
  {:bot-token   <bots secret token>
   :chat-id     <chat id>
   :method      <telegram api method> https://core.telegram.org/bots/api#available-methods
   :action      <chat action> https://core.telegram.org/bots/api#sendchataction
   :text        <text content of the message>
   :reply-to-id <id of the message to reply>
   :message-id  <id of the message to delete>
   :file        <path of the file to send>
   :width       <width of the video file>
   :height      <height of the video file>}"
  [command]

  (let [{bot-token :bot-token chat-id :chat-id method :method} command]
    (if (every? identity [bot-token chat-id method])
      (let [{action      :action
             text        :text
             reply-to-id :reply-to-id
             message-id  :message-id
             file        :file
             width       :width
             height      :height} command
            url (str (get-config-value :telegram-api-endpoint) bot-token "/" method)
            query-params {:chat_id             chat-id
                          :action              action
                          :text                text
                          :message_id          message-id
                          :reply_to_message_id reply-to-id
                          :width               width
                          :height              height}
            response (client/post url {:query-params     query-params
                                       :throw-exceptions false
                                       :multipart        (when file [{:name    "video"
                                                                      :content (clojure.java.io/file file)}])})
            status (:status response)]
        (log/info "Telegram command sent; method:" method "status:" status)
        status)
      (log/error "At least :bot-token, :chat-id and :method must be provided."))))