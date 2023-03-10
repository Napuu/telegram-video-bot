(ns telegram-video-download-bot.core
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.downloader :refer [start-downloader]]
            [telegram-video-download-bot.ingester :refer [start-server]])
  (:gen-class))

(defn start [args]
  (match (vec (map keyword args))
    [:ingester] (start-server)
    [:handler] (start-downloader)
    :else (do (log/error "No match, exiting")
              (System/exit 1))))

(defn -main [& args]
  (when (str/blank? (get-config-value :token))
    (log/error "Please provide TELEGRAM_TOKEN")
    (System/exit 1))
  (start args))
