(ns telegram-video-download-bot.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.shell :refer [sh]]
            [telegram-video-download-bot.util :refer [string-to-seconds-or-nil
                                                      parse-message
                                                      run-ffmpeg]]))

(deftest test-string-to-seconds-or-nil
  (testing "Parsing seconds from string\n"
    (doseq [[input expected-output] [["126" 126]
                                     ["26s" 26]
                                     ["1m26s" 86]
                                     ["kissa" nil]
                                     ["1m26" 86]
                                     ["0m" 0]
                                     ["1m" 60]
                                     ["2m" 120]
                                     ["-2m" -120]
                                     [nil nil]
                                     ["h,m100sllo" nil]]]
      (testing (str "Input: " input ", expected output: " (or expected-output "nil"))
        (is (= (string-to-seconds-or-nil input) expected-output))))))

(deftest test-parse-message
  (testing "Matching url captures content correctly"
    (doseq [[input expected-output] [["url dl" ["url" nil nil]]
                                     ["url 1 kissa dl" ["url" 1 nil]]
                                     ["url l" [nil nil nil]]
                                     ["urldl" [nil nil nil]]
                                     ["url dl 334" [nil nil nil]]
                                     ["url 1 2 dl" ["url" 1 2]]]]
      (testing (str "Input: " input ", expected output: " (or expected-output "nil"))
        (is (= (parse-message input "dl") expected-output))))))

(deftest ffmpeg-wrapper
  (testing "Calls ffmpeg with correct arguments to convert webm to mp4"
    (with-redefs [sh (fn [& args]
                       (is (= (str args) "((\"ffmpeg\" \"-y\" \"-i\" \"output.webm\" \"-fs\" \"45000000\" \"output.webm_new.mp4\"))"))
                       {:exit 0})]
      (is (= (run-ffmpeg "output.webm" nil nil)  "output.webm_new.mp4"))))
  (testing "Does not call ffmpeg if file is mp4 to begin with"
    (let [enqueue-called? (atom false)]
      ( with-redefs [sh (fn [& _]
                          (reset! enqueue-called? true)
                          {:exit 0})]
        (is (= @enqueue-called?  false) ) )))
  (testing "Calls ffmpeg with correct arguments to trim mp4 when start is defined"
    (with-redefs [sh (fn [& args]
                       (is (= (str args) "((\"ffmpeg\" \"-y\" \"-ss\" 100 \"-i\" \"output.mp4\" \"-fs\" \"45000000\" \"output.mp4_new.mp4\"))"))
                       {:exit 0})]
      (is (= (run-ffmpeg "output.mp4" 100 nil)  "output.mp4_new.mp4"))))
  (testing "Calls ffmpeg with correct arguments to trim mp4 when start and duration are defined"
    (with-redefs [sh (fn [& args]
                       (is (= (str args) "((\"ffmpeg\" \"-y\" \"-ss\" 100 \"-t\" 28 \"-i\" \"output.mp4\" \"-fs\" \"45000000\" \"output.mp4_new.mp4\"))"))
                       {:exit 0})]
      (is (= (run-ffmpeg "output.mp4" 100 28)  "output.mp4_new.mp4")))))
