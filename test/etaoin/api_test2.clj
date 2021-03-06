(ns etaoin.api-test2
  (:require [etaoin.api :refer :all]
            [etaoin.proc]
            [clojure.test :refer :all])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest test-firefox-driver-args
  (with-redefs
    [etaoin.proc/run  (fn [_ _])
     wait-running     identity
     create-session   (fn [_ _] "session-key")
     etaoin.proc/kill identity
     delete-session   identity]
    (testing "Session"
      (with-firefox {} driver
        (is (= "session-key"
               (:session @driver)))))
    (testing "No custom args"
      (with-firefox {:port 1234} driver
        (is (= ["geckodriver" "--port" 1234]
               (:args @driver)))))
    (testing "Default `--marionette-port` is assigned when `:profile` is specified"
      (with-firefox {:port 1234 :profile "/tmp/firefox-profile/1"} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2828]
               (:args @driver)))))
    (testing "Custom `--marionette-port` is assigned when `:profile` is specified"
      (with-firefox {:port        1234
                     :profile     "/tmp/firefox-profile/1"
                     :args-driver ["--marionette-port" 2821]} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2821]
               (:args @driver)))))))

(deftest test-chrome-profile
  (let [profile-path (str (Files/createTempDirectory
                            "chrome-profile"
                            (into-array FileAttribute [])))]
    (with-chrome {:profile profile-path :args ["--no-sandbox"]} driver
      (go driver "chrome://version")
      (is profile-path
          (get-element-text driver :profile_path)))))

(deftest test-fail-run-driver
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"wrong-driver-path"
        (chrome {:path-driver "wrong-driver-path"}))))
