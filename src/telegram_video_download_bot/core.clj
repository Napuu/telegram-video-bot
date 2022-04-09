(ns telegram-video-download-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [telegram-video-download-bot.downloader :refer [start-downloader]]
            [telegram-video-download-bot.ingester :refer [start-ingester]]
            [telegram-video-download-bot.telegram :as tg]
            [telegram-video-download-bot.util :as util])
  (:gen-class))

(def token (env :telegram-token))
(def target-dir (env :target-dir))
(def POSTFIX " dl")

(defn start-message-ingester []
  (println "starting ingester")
  (start-ingester))
(defn start-message-handler []
  (println "starting handler")
  (start-downloader))
(defn no-match []
  (println "no match, exiting")
  (System/exit 1))

(defn func [args]
  (match (vec (map keyword args))
    [:ingester] (start-message-ingester)
    [:handler] (start-message-handler)
    :else (no-match)))

(defn -main [& args]

  (func args)
  (println "haloo")
  (when (or (str/blank? token) (str/blank? target-dir))
    (println "Please provide TELEGRAM_TOKEN and TARGET_DIR environment variables")
    (System/exit 1))

  (println "Starting the telegram-video-download-bot")
  (println "target-dir" target-dir)
  (println "telegram-token" token)
  
  ; (start-polling)
  )
