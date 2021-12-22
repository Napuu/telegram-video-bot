(ns telegram-video-download-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io])
  (:gen-class))


(def token (env :telegram-token))
(def target-dir (env :target-dir))

(h/defhandler handler

  (h/message-fn
   (fn [{{id :id} :chat :as message}]
     (println "received message")
     ; (println message)
     (def message_id (:message_id message))
     (def patterns
       (list
        #"https\:\/\/vm.tiktok.com\/[a-zA-Z0-9]+"
        #"https\:\/\/www.youtube.com/watch\?v=[a-zA-Z0-9-_]+"
        #"https\:\/\/youtu.be/[a-zA-Z0-9-_]+"))

     (defn matching-url
       "return url that match against any of the patterns"
       [patterns text]
       (first (filter (fn [x] (not (nil? x)))
                      (flatten (map (fn [pattern] [(re-find pattern text)])
                                    patterns)))))

     (defn filename-to-full-path
       "Full path to the file"
       [filename]
       (str/join "/" [target-dir filename]))

     (defn download-file
       "Download file and return its locations on disk"
       [url]

       (println "youtube-dl query next")
       (def filename (str/trim (:out (sh "youtube-dl" "--get-filename" url))))
       (println "youtube-dl query done")
       (def full-path (filename-to-full-path filename))
       (if (.exists (io/file full-path))
         (println "File already exists: " full-path)
         (sh "youtube-dl" "-o" full-path url))
       (if (str/ends-with? full-path ".mp4")
         (println "File is mp4")
         (println "File is not mp4"))
       full-path)

     (defn send-video
       "Send video back to chat"
       [token id filename reply_id]
       (def curl-exit-code
         (:exit (sh "curl" "-q" "-F"
                    (str "video=@\"" (filename-to-full-path filename) "\"")
                    (str "https://api.telegram.org/bot" token "/sendVideo?chat_id=" id
                         (if reply_id (str "&reply_to_message_id=" reply_id) "")))))
       (if (= curl-exit-code 0)
         (println "Video sent")
         (println "Video sending failed")))

     (defn delete-original-message
       "Delete original message"
       [token id message_id]
       (sh "curl" (str "https://api.telegram.org/bot" token "/deleteMessage?chat_id=" id "&message_id=" message_id)))

     (defn send-original-text-no-preview
       "Send original text without preview"
       [token id text]
       (sh "curl" (str "https://api.telegram.org/bot" token "/sendMessage?disable_web_page_preview=true&chat_id=" id "&text=" text)))

     (defn send-video-and-edit-history
       "Send video and if it's succesful delete original message"
       [token id message_id filename message]
       (println "posting video...")
       (def reply_id (:message_id (:reply_to_message message)))
       (send-video token id filename reply_id)
       (delete-original-message token id message_id)
       ; Wonder what should be done with messages that contain link and some text
       ; (send-original-text-no-preview token id text)
       )

     (defn handle-success
       "Success, as in not nil message received"
       [patterns text message]
       (def should-dl (str/ends-with? text "dl"))
       (println should-dl)
       (def found-match (matching-url patterns text))
       (if (or (nil? found-match) (not should-dl))
         (println "nothing to send. no matches")
         (send-video-and-edit-history token id message_id (download-file found-match) message )))

     (defn handle-nil
       "Fail, as in nil message received"
       []
       (println "nil received"))

     (def text (:text message))
     (if (nil? text)
       (handle-nil)
       (handle-success patterns text message)))))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (<!! (p/start token handler)))
