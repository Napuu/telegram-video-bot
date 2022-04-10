(ns telegram-video-download-bot.telegram
  (:require [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.logging :as log]))

(defn send-video
  "Send video back to chat. Return error code if something went wrong."
  [token id filename message]
  (let [reply_id (:message_id (:reply_to_message message))
        sent_message (sh "curl" "-q" "-F"
                         (str "video=@\"" filename "\"")
                         (str "https://api.telegram.org/bot" token "/sendVideo?chat_id=" id
                              (if reply_id (str "&reply_to_message_id=" reply_id) "")))
        send_message_parsed (json/read-str (sent_message :out))
        sent_message_ok? (get send_message_parsed "ok")]
    (if (not sent_message_ok?)
      (get send_message_parsed "error_code")
      nil)))

(defn send-chat-action
  [token id action]
  (let [send-success (= (:exit
                         (sh "curl" "-q"
                             (str "https://api.telegram.org/bot" token "/sendChatAction?"
                                  "chat_id=" id
                                  "&action=" action))) 0)]
    (when (not send-success) (log/error "Sending chat action failed"))
    send-success))

(defn send-response-no-match
  "Send message about no matches back to chat. Return true on success, false otherwise"
  [token id reply-to-id text]
  (let [send-success (= (:exit
                         (sh "curl" "-q"
                             (str "https://api.telegram.org/bot" token "/sendMessage?"
                                  "chat_id=" id
                                  "&text=" text
                                  "&reply_to_message_id=" reply-to-id))) 0)]
    (log/error "File not sent")
    send-success))

(defn delete-original-message
  "Delete original message"
  [token id message_id]
  (sh "curl" (str "https://api.telegram.org/bot" token "/deleteMessage?chat_id=" id "&message_id=" message_id)))
