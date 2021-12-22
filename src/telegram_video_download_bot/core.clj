(ns telegram-video-download-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [telegram-video-download-bot.util :as util]
            [telegram-video-download-bot.telegram :as tg])
  (:gen-class))

(def token (env :telegram-token))
(def target-dir (env :target-dir))
(def POSTFIX " dl")

(h/defhandler handler

  (h/message-fn
   (fn [{{chat-id :id} :chat :as message}]
     (def message-id (:message_id message))

     (defn send-video-and-edit-history
       "Send video and if it succeeds, delete original message"
       [token id message_id filename message]
       (if (tg/send-video token id filename message)
         (tg/delete-original-message token id message_id)
         (println "Sending video failed" filename)))

     (defn handle-success
       "Success, as in not nil message received"
       [text message]
       (let [found-match (util/matching-url text POSTFIX)]
         (and (not (nil? found-match))
              (let [filename (util/download-file found-match target-dir)]
                (send-video-and-edit-history token chat-id message-id filename message)))))
       
     (defn handle-nil
       "Fail, as in nil message received, no logging though. Useful for debugging"
       []
       nil)

     (def text (:text message))
     (if (nil? text)
       (handle-nil)
       (handle-success text message)))))

(defn -main
  [& args]
  (when (or (str/blank? token) (str/blank? target-dir))
    (println "Please provde TELEGRAM_TOKEN and TARGET_DIR environment variables")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (<!! (p/start token handler)))
