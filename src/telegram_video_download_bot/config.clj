(ns telegram-video-download-bot.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(def filepath (or (env :config-file) "config.edn"))

(def config-map (merge
                 ; defaults
                 {:target-dir "/tmp"
                  :blacklist []
                  :postfix " dl"
                  :mq-host "localhost"
                  :mq-port "5672"
                  :base-error-message "Hyvä linkki......"}
                 ; if config file exists, load it and override defaults
                 (when (.exists (io/as-file filepath))
                   (edn/read-string (slurp filepath)))))

(defn get-config-value
  [key]
  (get config-map key))