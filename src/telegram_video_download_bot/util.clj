(ns telegram-video-download-bot.util
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clj-http.client :as client]
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

(def yt-dlp-base-args ["yt-dlp" "-S" "codec:h264" "--merge-output-format" "mp4"])

(defn get-redirect-url [url]
  "Returns Location header from response if it exists, original url otherwise."
  (try
    (let [new-url (-> url
                      (client/get {:redirect-strategy :none})
                      :headers
                      :location)]
      (if (str/blank? new-url)
        url
        new-url))
    (catch Exception _ url)))

(defn yt-dlp-get-filename [url]
  "Get filename for url with yt-dlp. Return nil if link is not downloadable."
  (let [filename (-> (apply sh (concat yt-dlp-base-args ["--get-filename" url]))
                     :out
                     str/trim)]
    (when (-> filename str/blank? not) filename)))

(defn yt-dlp-download-file [output-path url]
  "Download url to output-path with yt-dlp. Return exit code of yt-dlp."
  (-> (apply sh (concat yt-dlp-base-args ["-o" output-path url]))
      :exit))

(defn handle-non-mp4 [output-path]
  "Check that file is actually mp4. Returns path that is guaranteed to be mp4, nil on fail.

  Even though we specifically request mp4-file, something else,
  usually webm-file might be downloaded as well. In that scenario,
  it's converted to mp4 via ffmpeg."

  (if (str/ends-with? output-path ".mp4")
    output-path
    (let [new-output-path (str output-path ".mp4")
          ffmpeg-exit-code (-> (sh "ffmpeg" "-y" "-i" output-path new-output-path) :exit)]
      (when (= ffmpeg-exit-code 0)
        new-output-path))))

(defn download-file
  "Download file and return its locations on disk. Return nil on fail."
  [url target-dir]
  (log/info "Downloading file")
  ; get-redirect-url is needed below as sometimes yt-dlp can't follow redirects properly
  (let [redirect-fixed-url (get-redirect-url url)]
    (when-let [filename (yt-dlp-get-filename redirect-fixed-url)]
      (let [full-path (filename-to-full-path target-dir filename)
            yt-dlp-exit-code (yt-dlp-download-file full-path redirect-fixed-url)]
        (when (= yt-dlp-exit-code 0)
          (handle-non-mp4 full-path))))))

(comment
  (get-redirect-url "asdf"))