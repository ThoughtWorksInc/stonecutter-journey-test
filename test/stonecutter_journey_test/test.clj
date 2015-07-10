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
   (after  :contents (do (wd/quit)))]

  (fact "can go to client page"
        (wd/to "https://stonecutter-client.herokuapp.com")
        (wait-for-title "Home")
        (screenshot "client_home_page")
        (wd/current-url) => (contains "stonecutter-client.herokuapp.com/login"))

  (fact "'sign in to vote' redirects to stonecutter"
        (wd/click "button")
        (wait-for-title "Sign in")
        (screenshot "stonecutter_sign_in")
        (wd/current-url) => (contains "stonecutter.herokuapp.com/sign-in"))

  (fact "can sign in with existing user credentials and redirects to authorisation form page"
        (wd/input-text ".func--email__input" "stonecutter-journey-test@tw.com")
        (wd/input-text ".func--password__input" "password")
        (wd/click ".func--sign-in__button")
        (wait-for-title "Authorise")
        (screenshot "stonecutter_authorisation_form")
        (wd/current-url) => (contains "stonecutter.herokuapp.com/authorisation"))

  (fact "authorising app redirects to voting page"
        (wd/click ".func--authorise-share-profile__button")
        (wait-for-title "Poll: Soho requires safer cycle lanes?")
        (screenshot "client_voting_page")
        (wd/current-url) => (contains "stonecutter-client.herokuapp.com/voting")
        (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

  (fact "logging out in client app and logging in again will skip sign in page"
        (wd/click ".func--logout__link")
        (wait-for-title "Home")
        (wd/click "button")
        (wait-for-title "Authorise")
        (wd/click ".func--authorise-share-profile__button")
        (wait-for-title "Poll: Soho requires safer cycle lanes?")
        (wd/current-url) => (contains "stonecutter-client.herokuapp.com/voting")
        (wd/page-source) => (contains "stonecutter-journey-test@tw.com")))
