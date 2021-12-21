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
(def base-url (env :base-url))

(h/defhandler handler

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id "Welcome to telegram-video-download-bot!")))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (println "Help was requested in " chat)
                  (t/send-text token id "Help is on the way")))

  (h/message-fn
   (fn [{{id :id} :chat :as message}]
     (println "received message")
     (println message)
     (def message_id (:message_id message))
     (def patterns
       (list
        #"https\:\/\/vm.tiktok.com\/[a-zA-Z0-9]+"
        #"https\:\/\/www.youtube.com/watch\?v=[a-zA-Z0-9-_]+"
        #"https\:\/\/youtu.be/[a-zA-Z0-9-_]+"))

     ;(def text "https://youtu.be/e6MLjaKhp5U")
     (def text (:text message))
     (if (not (nil? text)) (
     (def matches
       (filter (fn [x] (not (nil? x)))
               (flatten
                (map
                 (fn [pattern] [(re-seq pattern text)])
                 patterns))))

     (if (> (count matches) 0)
(
     (def filenames
       (mapv (fn [url]
               (println "youtube-dl query next")
               (def filename (str/trim (:out (sh "youtube-dl" "-f" "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4" "--get-filename" url))))
               (println "youtube-dl query done")
               (if (.exists (io/file filename))
                 (println "File already exists: " filename)
                 (sh "youtube-dl" "-f" "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4" "-o" filename url))
               (sh "cp" filename target-dir)
               filename)
             matches))

     (run! (fn [filename]
             (t/send-video token id (apply str [base-url filename]))) filenames)

     ) (println "non matching message")))
        (println "nil")))
))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (<!! (p/start token handler)))
