(ns telegram-video-download-bot.ingester-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [telegram-video-download-bot.mq :refer [enqueue-link]]
            [telegram-video-download-bot.ingester :refer [ingest-telegram-message app]]))

(defn example-webhook-update [text]
  {:update_id 288704873,
   :message   {:message_id       123,
               :from             {:id 123123, :is_bot false, :first_name "Jack", :last_name "Black", :username "jb2"},
               :chat             {:id -123123, :title "test", :type "supergroup"},
               :reply_to_message {:message_id 456}
               :date             1651686555,
               :text             text}})

(def expected-response {:status  200
                        :headers {}
                        :body    "OK"})

(deftest webhook-handler-test-regular-message
  (testing "Test that `ingest-telegram-message` is called with correct parameters."
    (let [ingest-called? (atom false)
          webhook-update (example-webhook-update "normal message")]
      (with-redefs [ingest-telegram-message (fn [x]
                                              (reset! ingest-called? true)
                                              (is (= x (:message webhook-update))))]
        (is (= (app (-> (mock/request :post "/")
                        (mock/json-body webhook-update)))
               expected-response))
        (is (= true @ingest-called?)))))

  (testing "Test that message not ending to ' dl' is just ignored."
    (let [enqueue-called? (atom false)
          webhook-update (example-webhook-update "normal message")]
      (with-redefs [enqueue-link (fn [& {:keys []}]
                                   (reset! enqueue-called? true))]
        (is (= (app (-> (mock/request :post "/")
                        (mock/json-body webhook-update)))
               expected-response))
        (is (= false @enqueue-called?))))))

(deftest webhook-handler-test-actual-link
  (testing "Test that message ending ' dl' is correctly sent to the message queue"
    (let [enqueue-called? (atom false)
          expected-link "https://example.com/link"
          webhook-update (example-webhook-update (str expected-link " dl"))]
      (with-redefs [enqueue-link (fn [& {:keys [link chat-id start duration message-id reply-to-id]}]
                                   (reset! enqueue-called? true)
                                   (is (= link expected-link))
                                   (is (= start nil))
                                   (is (= duration nil))
                                   (is (= chat-id (:id (:chat (:message webhook-update)))))
                                   (is (= message-id (:message_id (:message webhook-update))))
                                   (is (= reply-to-id (:message_id (:reply_to_message (:message webhook-update))))))]
        (is (= (app (-> (mock/request :post "/")
                        (mock/json-body webhook-update)))
               expected-response) "Should respond with 200")
        (is (= true @enqueue-called?) "Should actually call enqueue-link")))))
