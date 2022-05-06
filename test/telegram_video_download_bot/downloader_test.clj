(ns telegram-video-download-bot.downloader_test
  (:require [clojure.test :refer :all]
            [telegram-video-download-bot.util :refer [download-file]]
            [telegram-video-download-bot.telegram :refer [send-telegram-command]]
            [telegram-video-download-bot.downloader :refer [message-handler]]))

; https://stackoverflow.com/a/45669319/1550017
(defn map-subset? [a-map b-map]
  (every? (fn [[k _ :as entry]] (= entry (find b-map k))) a-map))

(deftest message-handler-test
  (let [link "https://example.com/link"
        chat-id 123
        message-id 321
        reply-to-id 456
        mq-byte-response (.getBytes (format "{
        \"link\": \"%s\",
        \"chat-id\": %d,
        \"message-id\": %d,
        \"reply-to-id\": %d
        }" link chat-id message-id reply-to-id))]

    (testing "Successful send"
      (with-redefs [download-file (fn [url _]
                                    (is (= url link))
                                    "mock-location")
                    send-telegram-command (let [expected-arguments
                                                (atom [{:method "sendChatAction" :action "upload_video"}
                                                       {:method "sendVideo" :reply-to-id 456}
                                                       {:method "deleteMessage" :message-id 321}])]
                                            (fn [args] (let [expected (ffirst (swap-vals! expected-arguments rest))]
                                                         (testing "Make sure correct commands are sent to Telegram api"
                                                           (is (map-subset? expected args)))) 200))]
        (message-handler nil nil mq-byte-response)))

    (testing "Unsuccessful send"
      (with-redefs [download-file (fn [url _]
                                    (is (= url link))
                                    false)
                    send-telegram-command (let [expected-arguments
                                                (atom [{:args {:method "sendChatAction" :action "upload_video"} :resp 200}
                                                       {:args {:method "sendVideo" :reply-to-id 456} :resp 413}
                                                       {:args {:method "sendMessage"} :resp 200}])]
                                            (fn [args] (let [current (ffirst (swap-vals! expected-arguments rest))
                                                             expected-args (:args current)
                                                             status-code (:resp current)]
                                                         (testing "Make sure correct commands are sent to Telegram api,
                                                                   when file is too big."
                                                           (is (map-subset? expected-args args))) status-code)))]
        (message-handler nil nil mq-byte-response)))))
