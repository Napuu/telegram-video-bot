(ns telegram-video-download-bot.util
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

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
  [message blacklisted-words]
  (some (fn [word] (str/includes? message word)) blacklisted-words))

(defn download-file
  "Download file and return its locations on disk. Return false on fail."
  [url target-dir]
  (log/info "Downloading file")
  (let [filename (str/trim (:out (sh "yt-dlp" "-S" "codec:h264" "--get-filename" "--merge-output-format" "mp4" url)))
        full-path (filename-to-full-path target-dir filename)]
    ;; useful for debugging, don't want to have this enabled all the time
    ;; (log/debug url full-path)
    (and (not (.exists (io/file full-path)))
         (sh "yt-dlp" "-S" "codec:h264" "--merge-output-format" "mp4" "-o" full-path url))
    (if (str/ends-with? full-path ".mp4")
      full-path
      nil)))