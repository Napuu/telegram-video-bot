(ns telegram-video-download-bot.util
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [telegram-video-download-bot.config :refer [get-config-value]]))

(def max-size-in "500m")
(def max-size-out 45000000)

(defn filename-to-full-path
  "Full path to the file"
  [target-dir filename]
  (let [has-trailing-slash (str/ends-with? target-dir "/")
        target-dir-no-trailing (if has-trailing-slash
                                 (subs target-dir 0 (- (count target-dir) 1))
                                 target-dir)]
    (str/join "/" [target-dir-no-trailing filename])))

(defn string-to-integer-or [string default]
  (if (integer? string) string (try (Integer/parseInt string)
                                    (catch NumberFormatException _ default)) ))
(defn string-to-seconds-or-nil
  "Get total seconds from string, e.g., '1m25s'->85."
  [string]
  (if (nil? string) nil
    (let [minutes-split (str/split string #"m")
          has-minutes (str/includes? string "m")
          minutes-split-count (count minutes-split)
          seconds-split-after-minutes (str/split (str/join (or (next minutes-split) string)) #"s")
          seconds-split-count (count seconds-split-after-minutes)]
      (if (or (> minutes-split-count 2)
              (> seconds-split-count 1))
        nil ; string had more than one 's' or 'm' -> it cannot be parsed
        (string-to-integer-or
          (case [minutes-split-count seconds-split-count]
            [1 1] (if has-minutes (* (string-to-integer-or (first minutes-split) 0) 60)
                    (first seconds-split-after-minutes))
            [1 2] (first seconds-split-after-minutes)
            [2 1] (+ (* (string-to-integer-or (first minutes-split) 0) 60) (string-to-integer-or (first seconds-split-after-minutes) 0))
            nil) nil)) )))

(defn parse-message
  "Return triple (url, ts-start, duration), if text ends with @postfix, nil for not existing args."
  [text postfix]
  (let [split ( str/split text #" ")
        last-arg (last split)]
    (if (= last-arg postfix)
      (let [ [url start end] split]
        [url (string-to-seconds-or-nil start) (string-to-seconds-or-nil end)])
      [nil nil nil])))

(defn contains-blacklisted-word?
  "Return true if message contains blacklisted word, nil otherwise"
  [message]
  (let [blacklisted-words (get-config-value :blacklist)]
    (and (> 0 (count blacklisted-words))
         (some (fn [word] (str/includes? message word)) blacklisted-words))))

(def yt-dlp-additional-args "-f" "((bv*[fps>30]/bv*)[height<=720]/(wv*[fps>30]/wv*)) + ba / (b[fps>30]/b)[height<=720]/(w[fps>30]/w)")

(def yt-dlp-base-args ["yt-dlp"
                       "--merge-output-format" "mp4"
                       "--max-filesize" max-size-in
                       "-S" "codec:h264"])

(defn get-redirect-url
  "Returns Location header from response if it exists, original url otherwise."
  [url]
  (try
    (let [new-url (-> url
                      (client/get {:redirect-strategy :none})
                      :headers
                      :location)]
      (if (str/blank? new-url)
        url
        new-url))
    (catch Exception _ url)))

(defn yt-dlp-download-file
  "Download url to output-path with yt-dlp. Return exit code of yt-dlp."
  [output-path url try-additional-args]
  (-> (apply sh (concat yt-dlp-base-args ["-o" output-path url] (if try-additional-args yt-dlp-additional-args "")))
      :exit))

(defn file-too-big [filename]
  (> (.length (io/file filename)) max-size-out))

(defn run-ffmpeg
  "Check that file is actually mp4 and not too big. Also trims it.
   Returns path that is guaranteed to be mp4 of proper size, nil on fail.

  Even though we specifically request mp4-file, something else,
  usually webm-file might be downloaded as well. In that scenario,
  it's converted to mp4 via ffmpeg. '-fs'-flag is used to stop writing after file is big enough."
  [output-path start duration]

  (let [too-big? (file-too-big output-path)]
    (if (and (str/ends-with? output-path ".mp4") (not too-big?) (and (nil? start) (nil? duration)))
      output-path
      (let [new-output-path (str output-path "_new.mp4")
            ffmpeg-output (apply sh (remove nil? (flatten
                                                   ["ffmpeg" "-y"
                                                    (and start ["-ss" (str start)])
                                                    (and (and start duration) ["-t" (str duration)])
                                                    "-i" output-path "-fs" (str max-size-out) new-output-path]) ))
            ffmpeg-exit-code (:exit ffmpeg-output)]
        (when (= ffmpeg-exit-code 0)
          new-output-path)))))

; https://stackoverflow.com/a/27550676/1550017
(defn timeout [timeout-ms callback]
  (let [fut (future (callback))
        ret (deref fut timeout-ms nil)]
    (when (nil? ret)
      (future-cancel fut))
    ret))

(defn download-file-no-timeout
  [url target-dir try-additional-args start duration]
  (when-let [filename (str (java.util.UUID/randomUUID) ".mp4")]
    (let [full-path (filename-to-full-path target-dir filename)]
      (yt-dlp-download-file full-path url try-additional-args)
      (run-ffmpeg full-path start duration))))

(defn download-file
  "Download file and return its locations on disk. Return nil on fail."
  [url target-dir try-additional-args start duration]
  (log/info "Downloading file")
  (timeout (get-config-value :timeout-milliseconds) 
           #(download-file-no-timeout url target-dir try-additional-args start duration)))

(defn get-video-dimensions
  [path]
  ; https://stackoverflow.com/a/29585066/1550017
   (let [out (str/split (:out (sh "ffprobe" "-v" "error" "-select_streams" "v" "-show_entries" "stream=width,height" "-of" "csv=p=0:s=x" path)) #"x")
         width (first out)
         height (first (str/split (second out) #"\n"))]
      [(Integer/parseInt width) (Integer/parseInt height)]))
