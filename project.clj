(defproject telegram-video-download-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [environ "1.1.0"]
                 [org.clojure/data.json "2.4.0"]
                 [ring "1.9.5"]
                 [ring/ring-mock "0.4.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-api "1.7.32"]
                 [org.slf4j/slf4j-log4j12 "1.7.32"]
                 [org.clojure/core.match "1.0.0"]
                 [clj-http "3.12.3"]
                 [ring/ring-json "0.5.1"]
                 [com.novemberain/langohr "5.1.0"]]

  :plugins [[lein-environ "1.1.0"]
            [lein-ring "0.12.5"]]

  :ring {:handler telegram-video-download-bot.ingester/app}

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
