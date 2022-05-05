(ns telegram-video-download-bot.ingester
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [enqueue-link]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.util :refer [contains-blacklisted-word? matching-url]]))

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
          (send-telegram-command {:bot-token (get-config-value :token) :chat-id chat-id
                                  :method "sendMessage"
                                  :reply-to-id message-id
                                  :text (get-config-value :base-error-message)}))))))
(defn handler [request]
  (ingest-telegram-message (:message (:body request)))
  (response "OK"))

(def app
  (wrap-json-body handler {:keywords? true}))

(comment
  (enqueue-link
    :link "localhost123"
    :chat-id -1001764073348
    :message-id 1083))

(defn start-server [& args]
  (run-jetty app {:port (Integer/valueOf (or (System/getenv "port") "3020"))}))