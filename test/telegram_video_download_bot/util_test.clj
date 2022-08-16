(ns telegram-video-download-bot.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [telegram-video-download-bot.util :refer [string-to-seconds-or-nil
                                                      parse-message]]))

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
