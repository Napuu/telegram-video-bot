(ns telegram-video-download-bot.ingester
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.mq :refer [enqueue-link]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.util :refer [contains-blacklisted-word? parse-message]]))

(defn ingest-telegram-message [message]
  (when (not (nil? (:text message)))
    (let [[link start duration] (parse-message (:text message) (get-config-value :postfix))
          contains-blacklisted-word (and link (contains-blacklisted-word? link))
          chat-id (:id (:chat message))
          message-id (:message_id message)
          reply-to-id (:message_id (:reply_to_message message))]
      (when link
        (if (not contains-blacklisted-word)
          (enqueue-link
            :link link
            :start start
            :duration duration
            :chat-id chat-id
            :message-id message-id
            :reply-to-id reply-to-id)
          (send-telegram-command {:bot-token   (get-config-value :token) :chat-id chat-id
                                  :method      "sendMessage"
                                  :reply-to-id message-id
                                  :text        (get-config-value :base-error-message)}))))))

(def app
  (wrap-json-body
    (fn [request]
      (ingest-telegram-message (:message (:body request)))
      (response "OK")) {:keywords? true}))

(defn start-server [& _]
  (run-jetty app {:port (Integer/parseInt (or (System/getenv "port") "3000"))}))