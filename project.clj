(defproject telegram-video-download-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ             "1.1.0"]
                 [org.clojure/data.json "2.4.0"]
                 [morse               "0.2.4"]
                 [org.clojure/core.match "1.0.0"]
                 [com.novemberain/langohr "5.1.0"]]

  :plugins [[lein-environ "1.1.0"]]

  :main ^:skip-aot telegram-video-download-bot.core
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
