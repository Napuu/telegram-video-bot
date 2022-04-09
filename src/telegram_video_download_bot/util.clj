(ns telegram-video-download-bot.util
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as car-mq]))

(defn filename-to-full-path
  "Full path to the file"
  [target-dir filename]
  (let [has-trailing-slash (str/ends-with? target-dir "/")
        target-dir-no-trailing (if has-trailing-slash
                                 (subs target-dir 0 (- (count target-dir) 1))
                                 target-dir)]
    (str/join "/" [target-dir-no-trailing filename])))

(defn matching-url
  "Return url with postfix stripped, if postfix exists, nil otherwise"
  [text postfix]
  (or (and (str/ends-with? text postfix)
           (first (str/split text (re-pattern postfix))))
      nil))

(defn now [] (java.util.Date.))

(defn download-file
  "Download file and return its locations on disk. Return false on fail."
  [url target-dir]
  (println (now) "Downloading file")
  (let [filename (str/trim (:out (sh "yt-dlp" "-S" "codec:h264" "--get-filename" "--merge-output-format" "mp4" url)))
        full-path (filename-to-full-path target-dir filename)]
    ;; useful for debugging, don't want to have this enabled all the time
    ;; (println url full-path)
    (and (not (.exists (io/file full-path)))
         (sh "yt-dlp" "-S" "codec:h264" "--merge-output-format" "mp4" "-o" full-path url))
    (if (str/ends-with? full-path ".mp4")
      full-path
      nil)))

(comment
  (def message-keys ["link" "chat-id" "reply-to-id"])
  (defn message-is-valid-for-queue
    [{:keys [link chat-id]}]
    (if (not (every? identity [link chat-id]))
      (or (println "Make sure 'link' and 'chat-id' are set") false)
      true))

  (message-is-valid-for-queue {:link "asdf" :chat-id "koira" :reply-to-id "haloo"})
  (message-is-valid-for-queue {:chat-id "koira" :reply-to-id "haloo"})
  (pprint/pprint "moi")
  (defn parse-link-from-queue
    "Parses link that was received from
     message queue"
    [{:keys [link chat-id reply-to-id]}]
    (if (not link)
      (println "No 'link' provided"))
    (println link chat-id reply-to-id))
  (defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

  (def my-worker
    (car-mq/worker {:pool {} :spec {:uri "http://localhost:6379"}} LINK_QUEUE
                   {:handler (fn [{:keys [message]}]
                               (parse-link-from-queue message)
                               {:status :success})}))


  (parse-link-from-queue {:link "link1", :chat-id "chat-id1", :reply-to-id "reply-to-id1"})
  (defn enqueue-link
    "Send link to message queue
     link - link to the actual video
     chat-id - Telegram's chat id
     reply-to-id - if exists, video was sent as a reply"
    [link chat-id reply-to-id]
    (wcar* (car-mq/enqueue LINK_QUEUE {:link link :chat-id chat-id :reply-to-id reply-to-id})))

  (def server1-conn {:pool {} :spec {:uri "http://localhost:6379"}})

  (enqueue-link "link1" "chat-id1" "reply-to-id1")

  (defn get-config
    [filepath key]
    (get (edn/read-string (slurp filepath)) key))
  )

(defn contains-blacklisted-word?
  "Return true if message contains blacklisted word, nil otherwise"
  [message blacklisted-words]
  (some (fn [word] (str/includes? message word)) blacklisted-words))

(comment
  (spit "testing" "filu")
  (slurp "testing")
  "haloo"
  (def blacklist (get-config "/tmp/config.edn" :blacklist))
  (contains-blacklisted-word? "locdalhost" blacklist)
  )