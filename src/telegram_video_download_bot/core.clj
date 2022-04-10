(ns telegram-video-download-bot.core
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.downloader :refer [start-downloader]]
            [telegram-video-download-bot.ingester :refer [start-ingester]]))

(defn start-message-ingester []
  (println "starting ingester")
  (start-ingester))
(defn start-message-handler []
  (println "starting handler")
  (start-downloader))
(defn no-match []
  (println "no match, exiting")
  (System/exit 1))

(defn start [args]
  (match (vec (map keyword args))
    [:ingester] (start-message-ingester)
    [:handler] (start-message-handler)
    :else (no-match)))

(defn -main [& args]
  (when (str/blank? (get-config-value :token))
    (println "Please provide TELEGRAM_TOKEN")
    (System/exit 1))
  (start args))
