(ns telegram-video-download-bot.telegram 
  (:require [clojure.java.shell :refer [sh]]
            [telegram-video-download-bot.util :refer [now]]))

(defn send-video
  "Send video back to chat. Return true on success, false otherwise"
  [token id filename message]
  (let [reply_id (:message_id (:reply_to_message message))
        send-success (= (:exit
                         (sh "curl" "-q" "-F"
                             (str "video=@\"" filename "\"")
                             (str "https://api.telegram.org/bot" token "/sendVideo?chat_id=" id
                                  (if reply_id (str "&reply_to_message_id=" reply_id) "")))) 0)]
    (println (now) "File sent")
    send-success))

(defn delete-original-message
  "Delete original message"
  [token id message_id]
  (sh "curl" (str "https://api.telegram.org/bot" token "/deleteMessage?chat_id=" id "&message_id=" message_id)))
