(ns telegram-video-download-bot.config
  (:require [environ.core :refer [env]]
            [clojure.string :as str]))

(def config-map {:token                 (env :token)
                 :target-dir            (or (env :target-dir) "/tmp")
                 :blacklist             (str/split (or (env :blacklist) "") #";")
                 :postfix               (or (env :postfix) "dl")
                 :mq-host               (or (env :mq-host) "localhost")
                 :mq-port               (Integer/parseInt (or (env :mq-port) "5672"))
                 :base-error-message    (or (env :base-error-message) "Hyvä linkki......")
                 :timeout-milliseconds  (Integer/parseInt (or (env :timeout-milliseconds) "200000"))
                 :telegram-api-endpoint (or (env :telegram-api-endpoint) "https://api.telegram.org/bot")})

(defn get-config-value
  [key]
  (get config-map key))
