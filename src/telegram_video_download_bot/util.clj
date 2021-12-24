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
  "Download file and return its locations on disk. Return false on fail."
  [url target-dir]
  (println (now) "Downloading file")
  (let [filename (str/trim (:out (sh "yt-dlp" "-S" "codec:h264" "--get-filename" "--merge-output-format" "mp4" url)))
        full-path (filename-to-full-path target-dir filename)]
    ; useful for debugging, don't want to have this enabled all the time
    ; (println url full-path)
    (and (not (.exists (io/file full-path)))
      (sh "yt-dlp" "-S" "codec:h264" "--merge-output-format" "mp4" "-o" full-path url))
    (if (str/ends-with? full-path ".mp4")
      full-path
      nil)))
  
