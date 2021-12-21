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

     (def patterns
       (list
        #"https\:\/\/www.youtube.com/watch\?v=[a-zA-Z0-9-_]+"
        #"https\:\/\/youtu.be/[a-zA-Z0-9-_]+"))

     ;(def text "https://youtu.be/e6MLjaKhp5U")
     (def text (:text message))
     (def matches
       (filter (fn [x] (not (nil? x)))
               (flatten
                (map
                 (fn [pattern] [(re-seq pattern text)])
                 patterns))))

     (def filenames
       (mapv (fn [url]
               (def filename (str/trim (:out (sh "youtube-dl" "--get-filename" url))))
               (if (.exists (io/file filename))
                 (println "File already exists: " filename)
                 (sh "youtube-dl" "-o" filename url))
               (sh "mv" filename target-dir)
               filename)
             matches))

     (run! (fn [filename]
             (t/send-video token id (apply str [base-url filename]))) filenames)

     )))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (<!! (p/start token handler)))
