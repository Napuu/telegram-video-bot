(ns telegram-video-download-bot.ingester 
  (:require [clojure.core.async :refer [<!!]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [enqueue-link
                                                    get-rmq-connection]]
            [telegram-video-download-bot.telegram :refer [send-response-no-match]]
            [telegram-video-download-bot.util :refer [contains-blacklisted-word? matching-url]]))


(h/defhandler handler
  (def conn (get-rmq-connection))
  (h/message-fn
   (fn [message]
     (let [link (matching-url (:text message) (get-config-value :postfix))
           contains-blacklisted-word (and link (contains-blacklisted-word? link (get-config-value :blacklist)))
           chat-id (:id (:chat message))
           message-id (:message_id message)
           reply-to-id (:message_id (:reply_to_message message))]
       (when link
         (if (not contains-blacklisted-word)
           (enqueue-link
            :conn conn
            :link link
            :chat-id chat-id
            :message-id message-id
            :reply-to-id reply-to-id)
           (send-response-no-match (get-config-value :token) chat-id message-id (get-config-value :base-error-message))))))))

(defn start-ingester []
  (<!! (p/start (get-config-value :token) handler)))

; Morse library does not handle situations well if Telegram API
; returns 502, see https://github.com/Otann/morse/issues/44
; To fix this, we catch all exceptions and try again after a while.
; note - might be fixed right now, so it's commented out
(comment (Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ _ throwable]
     (def msg (.getMessage throwable))
     (println msg)
     (if (= msg "clj-http: status 502")
       (Thread/sleep 5000)
       (Thread/sleep 60000))
     (start-ingester)))))