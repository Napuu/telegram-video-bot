(ns telegram-video-download-bot.core
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [telegram-video-download-bot.config :refer [get-config-value]]
            [telegram-video-download-bot.downloader :refer [start-downloader]]
            [telegram-video-download-bot.ingester :refer [start-server]])
  (:gen-class))

(defn start-message-ingester []
  (log/info "Starting ingester")
  (start-server))

(defn start-message-handler []
  (log/info "Starting handler")
  (start-downloader))

(defn no-match []
  (log/error "No match, exiting")
  (System/exit 1))

(defn start [args]
  (match (vec (map keyword args))
    [:ingester] (start-message-ingester)
    [:handler] (start-message-handler)
    :else (no-match)))

(defn -main [& args]
  (when (str/blank? (get-config-value :token))
    (log/error "Please provide TELEGRAM_TOKEN")
    (System/exit 1))
  (start args))
