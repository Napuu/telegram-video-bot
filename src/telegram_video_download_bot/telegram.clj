(ns telegram-video-download-bot.telegram
  (:require [clojure.java.shell :refer [sh]]
            [telegram-video-download-bot.util :refer [now]]))

(defn send-video
  "Send video back to chat. Return true on success, false otherwise"
  [token id filename message]

  (let [reply_id (:message_id (:reply_to_message message))
        sent_message (sh "curl" "-q" "-F"
                         (str "video=@\"" filename "\"")
                         (str "https://api.telegram.org/bot" token "/sendVideo?chat_id=" id
                              (if reply_id (str "&reply_to_message_id=" reply_id) "")))
        send-success (= (:exit sent_message) 0)]
    (println (now) "File sent")
    send-success))

(defn send-chat-action
  [token id action]
  (let [send-success (= (:exit
                         (sh "curl" "-q"
                             (str "https://api.telegram.org/bot" token "/sendChatAction?"
                                  "chat_id=" id
                                  "&action=" action))) 0)]
    (when (not send-success) (println (now) "Sending chat action failed"))
    send-success))

(comment
  (send-chat-action "5146505461:AAF3vkhitoyamx_EIwNlV9zGx_pZaAeHW_4" "-1001764073348" "upload_video"))
(defn send-response-no-match
  "Send message about no matches back to chat. Return true on success, false otherwise"
  [token id reply-to-id]
  (let [send-success (= (:exit
                         (sh "curl" "-q"
                             (str "https://api.telegram.org/bot" token "/sendMessage?"
                                  "chat_id=" id
                                  "&text=" "Hyvä linkki......"
                                  "&reply_to_message_id=" reply-to-id))) 0)]
    (println (now) "File not sent")
    send-success))

(defn delete-original-message
  "Delete original message"
  [token id message_id]
  (sh "curl" (str "https://api.telegram.org/bot" token "/deleteMessage?chat_id=" id "&message_id=" message_id)))
