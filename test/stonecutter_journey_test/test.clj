(ns stonecutter-journey-test.test
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clj-webdriver.taxi :as wd]
            [clj-webdriver.core :as wc]))

(defn wait-for-title [title]
  (try
    (wd/wait-until #(= (wd/title) title) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>>> Title never appeared:"))
      (prn (str "Expected: " title))
      (prn (str "Actual: " (wd/title)))
      (throw e))))

(def screenshot-directory "test/stonecutter_journey_test/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [filename]
  (prn (str "Screenshot: " filename))
  (wd/take-screenshot :file (str screenshot-directory "/"
                                 (format "%02d" (swap! screenshot-number + 1))
                                 "_" filename ".png")))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))

(against-background
  [(before :contents (do (wd/set-driver! {:browser :firefox})
                         (clear-screenshots)))
   (after :contents (do (wd/quit)))]

  (fact "can go to client page"
        (wd/to "http://stonecutter-client.herokuapp.com")
        (wait-for-title "Home")
        (screenshot "client_home_page")
        (wd/current-url) => "http://stonecutter-client.herokuapp.com/login"))
