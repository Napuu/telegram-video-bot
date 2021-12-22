(ns telegram-video-download-bot.util
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]))

(defn filename-to-full-path
  "Full path to the file"
  [target-dir filename]
  (str/join "/" [target-dir filename]))

(defn matching-url
  "Return url with postfix stripped, if postfix exists, nil otherwise"
  [text postfix]
  (or (and (str/ends-with? text postfix) (first (str/split text (re-pattern postfix))))
      nil))

(defn now [] (java.util.Date.))

(defn download-file
  "Download file and return its locations on disk"
  [url target-dir]
  (println (now) "Downloading file")
  (let [filename (str/trim (:out (sh "youtube-dl" "--get-filename" url)))
        full-path (filename-to-full-path target-dir filename)]
    (and (not (.exists (io/file full-path)))
      (sh "youtube-dl" "-o" full-path url))
    (and (not (str/ends-with? full-path ".mp4"))
      (println "File is not mp4, this might cause problems" full-path))
    full-path))
  