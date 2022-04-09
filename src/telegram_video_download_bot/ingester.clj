(ns telegram-video-download-bot.ingester 
  (:require [clojure.core.async :refer [<!!]]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [telegram-video-download-bot.mq :refer [enqueue-link]]
            [telegram-video-download-bot.util :refer [matching-url]]))

(def token (env :telegram-token))
(def POSTFIX " dl")

(h/defhandler handler
  (h/message-fn
   (fn [message]
     (let [link (matching-url (:text message) POSTFIX)]
       (when link
         (enqueue-link
          :link link
          :chat-id (:id (:chat message))
          :message-id (:message_id message)
          :reply-to-id (:message_id (:reply_to_message message))))
       ; (println "msg parsed")
       ))))

(defn start-ingester []
  (<!! (p/start token handler)))

; Morse library does not handle situations well if Telegram API
; returns 502, see https://github.com/Otann/morse/issues/44
; To fix this, we catch all exceptions and try again after a while.
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ _ throwable]
     (def msg (.getMessage throwable))
     (println msg)
     (if (= msg "clj-http: status 502")
       (Thread/sleep 5000)
       (Thread/sleep 60000))
     (start-ingester))))