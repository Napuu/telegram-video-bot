(ns telegram-video-download-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
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

     (defn matching-urls
       "return urls that match against the patterns"
       [patterns text]
       (filter (fn [x] (not (nil? x)))
               (flatten
                (map
                 (fn [pattern] [(re-seq pattern text)])
                 patterns))))

     (defn filename-to-full-path
       "Full path to the file"
       [filename]
       (str/join "/" [target-dir filename]))

     (defn download-files
       "Download list of files and return their locations on disk"
       [urls]
       (mapv (fn [url]
               (println "youtube-dl query next")
               (def filename (str/trim (:out (sh "youtube-dl" "-f" "bestvideo[ext=mp4]" "--get-filename" url))))
               (println "youtube-dl query done")
               (if (.exists (io/file filename))
                 (println "File already exists: " filename)
                 (sh "youtube-dl" "-f" "bestvideo[ext=mp4]" "-o" (filename-to-full-path filename) url))
               filename)
             urls))

     (defn send-video
       "Send video back to chat"
       [token id filename]
       (def curl-exit-code
         (:exit (sh "curl" "-q" "-F" (str "video=@\"" (filename-to-full-path filename) "\"") (str "https://api.telegram.org/bot" token "/sendVideo?chat_id=" id))))
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

     (defn send-video-and-delete-message
       "Send video and if it's succesful delete original message"
       [token id message_id filename text]
       (println "posting video...")
       (send-video token id filename)
       (delete-original-message token id message_id)
       (send-original-text-no-preview token id text))

     (defn handle-success
       "Success, as in not nil message received"
       [patterns text message]
       (println "received " text)
       (def ready-files (download-files
                         (matching-urls patterns text)))
       (if (= "bingchilling" (:title (:chat message)))
         (send-video-and-delete-message token id message_id (first ready-files) text)
         (println "non bing chilling"))
       ;(if (> (count ready-files) 8)
       ;  (delete-original-message token id message_id)
       ;  (println "nothing to delete")
       ;  )
       )

     (defn handle-nil
       "Fail, as in nil message received"
       []
       (println "nil received"))

     (def text (:text message))
     (if (nil? text)
       (handle-nil)
       (handle-success patterns text message))
     )))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (<!! (p/start token handler)))
