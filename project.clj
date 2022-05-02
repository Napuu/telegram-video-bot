(defproject telegram-video-download-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ             "1.1.0"]
                 [org.clojure/data.json "2.4.0"]
                 [morse               "0.2.4"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-api "1.7.32"]
                 [org.slf4j/slf4j-log4j12 "1.7.32"]
                 [org.clojure/core.match "1.0.0"]
                 [clj-http "3.12.3"]
                 [cheshire "5.9.0"]
                 [com.novemberain/langohr "5.1.0"]]

  :plugins [[lein-environ "1.1.0"]]

  :main ^:skip-aot telegram-video-download-bot.core
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
