(ns telegram-video-download-bot.dev
  (:require [telegram-video-download-bot.core :refer [start-message-ingester
                                                      start-message-handler]]))

(def example-message {:message_id 123,
                      :from       {:id 123123, :is_bot false, :first_name "John", :last_name "Smith", :username "john_smith"},
                      :chat       {:id -123321, :title "chat title", :type "supergroup"}, :date 1650223406, :text "test123 dl"})

(comment
  ; start ingester in own thread
  (.start (Thread. start-message-ingester))
  ; start handler in own thread
  (.start (Thread. start-message-handler))
  )