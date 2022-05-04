(ns telegram-video-download-bot.ingester
  (:require [clojure.core.async :refer [<!!]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [enqueue-link]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.util :refer [contains-blacklisted-word? matching-url]]))

(use 'ring.adapter.jetty)
(comment
  (run-jetty handler {:port 3000
                      :join? false}))

(defn handler [request]
  (println (:body request))
  (response "OK"))

(def app
  (wrap-json-body handler {:keywords? true}))

(defn ingest-telegram-message [message]
  (when (not (nil? (:text message)))

    (let [link (matching-url (:text message) (get-config-value :postfix))
          contains-blacklisted-word (and link (contains-blacklisted-word? link (get-config-value :blacklist)))
          chat-id (:id (:chat message))
          message-id (:message_id message)
          reply-to-id (:message_id (:reply_to_message message))]

      (when link
        (if (not contains-blacklisted-word)
          (enqueue-link
           :link link
           :chat-id chat-id
           :message-id message-id
           :reply-to-id reply-to-id)
          (send-telegram-command {:bot-token (get-config-value :token)
                                  :chat-id chat-id
                                  :method "sendMessage"
                                  :reply-to-id message-id
                                  :text (get-config-value :base-error-message)}))))))

(comment
  (enqueue-link
    :link "localhost123"
    :chat-id -1001764073348
    :message-id 1083))

(h/defhandler telegram-message-handler
  (h/message-fn
    (fn [message] (ingest-telegram-message message))))

(defn start-ingester []
  (<!! (p/start (get-config-value :token) telegram-message-handler)))

; Morse library does not handle situations well if Telegram API
; returns 502, see https://github.com/Otann/morse/issues/44
; To fix this, we catch all exceptions and try again after a while.
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ _ throwable]
     (let [msg (.getMessage throwable)]
       (println msg)
       (if (= msg "clj-http: status 502")
         (Thread/sleep 5000)
         (Thread/sleep 60000))
       (start-ingester)))))