(ns telegram-video-download-bot.config
  (:require [environ.core :refer [env]]
            [clojure.string :as str]))

(def config-map {:bot-token             (env :bot-token)
                 :target-dir            (or (env :target-dir) "/tmp")
                 :blacklist             (or (str/split (env :blacklist) #";") [])
                 :postfix               (or (env :postfix) " dl")
                 :mq-host               (or (env :mq-host) "localhost")
                 :mq-port               (or (Integer/parseInt (env :mq-port)) 5672)
                 :base-error-message    (or (env :base-error-message) "Hyvä linkki......")
                 :telegram-api-endpoint (or (env :telegram-api-endpoint) "https://api.telegram.org/bot")})

(defn get-config-value
  [key]
  (get config-map key))