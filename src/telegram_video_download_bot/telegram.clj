(ns telegram-video-download-bot.telegram
  (:require [telegram-video-download-bot.config :refer [get-config-value]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]))

(defn send-telegram-command
  "Send command to Telegram api. Takes dict as a parameter.
  {:bot-token   <bots secret token>
   :chat-id     <chat id>
   :method      <telegram api method> https://core.telegram.org/bots/api#available-methods
   :action      <chat action> https://core.telegram.org/bots/api#sendchataction
   :text        <text content of the message>
   :reply-to-id <id of the message to reply>
   :file        <path of the file to send>}"
  [command]

  (let [{bot-token :bot-token chat-id :chat-id method :method} command]
    (if (every? identity [bot-token chat-id method])
      (let [{action      :action
             text        :text
             reply-to-id :reply-to-id
             message-id  :deleted-message-id
             file        :file} command
            url (str (get-config-value :telegram-api-endpoint) bot-token "/" method)
            query-params {:chat_id             chat-id
                          :action              action
                          :text                text
                          :message-id          message-id
                          :reply_to_message_id reply-to-id}
            response (client/post url {:query-params     query-params
                                       :throw-exceptions false
                                       :multipart        (when file [{:name    "video"
                                                                      :content (clojure.java.io/file file)}])})
            status (:status response)]
        (when (not (= status 200)) status))
      (log/error "At least :bot-token, :chat-id and :method must be provided."))))

(comment
  ; "5146505461:AAF3vkhitoyamx_EIwNlV9zGx_pZaAeHW_4" -1001764073348 "upload_video"
  (send-telegram-command {:bot-token "5146505461:AAF3vkhitoyamx_EIwNlV9zGx_pZaAeHW_4"
                          :method    "sendMessage"
                          :chat-id   -10013764073348
                          :text      "asdf"})
  (send-telegram-command {:bot-token "5146505461:AAF3vkhitoyamx_EIwNlV9zGx_pZaAeHW_4"
                          :method    "sendMessage"
                          :chat-id   -10013764073348
                          :text      "asdf"})
  (send-telegram-command {:bot-token "5146505461:asdf"
                          :method    "sendMessage"
                          :text      "asdf"})
  (if-let [kissa 1]
    (println "asdf")
    (println "not"))
  (send-telegram-command {:bot-token "5146505461:AAF3vkhitoyamx_EIwNlV9zGx_pZaAeHW_4"
                          :method    "sendVideo"
                          :chat-id   -1001764073348
                          :file      "/tmp/kissa.mp4"})
  )