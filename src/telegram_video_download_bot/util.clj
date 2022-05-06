(ns telegram-video-download-bot.util
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [telegram-video-download-bot.config :refer [get-config-value]]))

(defn filename-to-full-path
  "Full path to the file"
  [target-dir filename]
  (let [has-trailing-slash (str/ends-with? target-dir "/")
        target-dir-no-trailing (if has-trailing-slash
                                 (subs target-dir 0 (- (count target-dir) 1))
                                 target-dir)]
    (str/join "/" [target-dir-no-trailing filename])))

(defn matching-url
  "Return url with postfix stripped, if postfix exists, nil otherwise"
  [text postfix]
  (or (and (str/ends-with? text postfix)
           (first (str/split text (re-pattern postfix))))
      nil))

(defn contains-blacklisted-word?
  "Return true if message contains blacklisted word, nil otherwise"
  [message]
  (let [blacklisted-words (get-config-value :blacklist)]
    (and (> 0 (count blacklisted-words))
         (some (fn [word] (str/includes? message word)) blacklisted-words))))

(defn download-file
  "Download file and return its locations on disk. Return nil on fail."
  [url target-dir]
  (log/info "Downloading file")
  (let [filename (str/trim (:out (sh "yt-dlp" "-S" "codec:h264" "--get-filename" "--merge-output-format" "mp4" url)))
        full-path (filename-to-full-path target-dir filename)]
    (sh "yt-dlp" "-S" "codec:h264" "--merge-output-format" "mp4" "-o" full-path url)
    (if (str/ends-with? filename ".mp4")
      ;; if filename ends with ".mp4", no additional conversion is needed
      full-path
      ;; if not, it is one of two scenarios,
      ;; 1. no file was found
      ;; 2. only webm or some other non-mp4 format was available
      (when (and (not (str/blank? filename))
                 (.exists (io/as-file full-path)))
        (do (sh "ffmpeg" "-i" full-path (str full-path ".mp4"))
            (str full-path ".mp4"))))))