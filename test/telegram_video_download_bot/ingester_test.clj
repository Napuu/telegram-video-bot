(ns telegram-video-download-bot.ingester-test
  (:require [clojure.test :refer :all])
  (:require [telegram-video-download-bot.ingester :refer [ingest-telegram-message]]))

(def example-message {:message_id 123,
                      :from       {:id 123123, :is_bot false, :first_name "John", :last_name "Smith", :username "john_smith"},
                      :chat       {:id -123321, :title "chat title", :type "supergroup"}, :date 1650223406, :text "test123 dl"})
(deftest ingest-telegram-message-test
  (is (= 5 (+ 2 2)))
  (comment
    ingest-telegram-message [example-message])
  )
