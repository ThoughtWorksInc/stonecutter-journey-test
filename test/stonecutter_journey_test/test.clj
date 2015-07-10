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

(defn wait-for-selector [selector]
  (try
    (wd/wait-until #(not (empty? (wd/css-finder selector))) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>> Selector could not be found: " selector))
      (prn "==========  PAGE SOURCE ==========")
      (prn (wd/page-source))
      (prn "==========  END PAGE SOURCE ==========")
      (throw e))))

(def screenshot-directory "test/stonecutter_journey_test/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [page]
  (let [filename (str (format "%02d" (swap! screenshot-number + 1)) "_" page ".png")
        filepath (io/file screenshot-directory filename)]
    (prn (str "Screenshot: " filepath))
    (wd/take-screenshot :file filepath)))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))

(def stonecutter-url "stonecutter.herokuapp.com")
(def stonecutter-sign-in-page-body ".func--sign-in-page")
(def stonecutter-sign-in-page-register-link ".func--register__link")
(def stonecutter-sign-in-email-input ".func--email__input")
(def stonecutter-sign-in-password-input ".func--password__input")
(def stonecutter-sign-in-button ".func--sign-in__button")
(def stonecutter-register-page-body ".func--register-page")
(def stonecutter-register-email-input ".func--email__input")
(def stonecutter-register-password-input ".func--password__input")
(def stonecutter-register-confirm-password-input ".func--confirm-password__input")
(def stonecutter-register-create-profile-button ".func--create-profile__button")
(def stonecutter-profile-created-page-body ".func--profile-created-page")
(def stonecutter-authorise-page-body ".func--authorise-page")
(def stonecutter-authorise-share-profile-button ".func--authorise-share-profile__button")
(def stonecutter-delete-account-page-body ".func--delete-account-page")
(def stonecutter-delete-account-button ".func--delete-account__button")
(def stonecutter-profile-deleted-page-body ".func--profile-deleted-page")

(def client-home-page-body ".func--home-page")
(def client-poll-page-body ".func--poll-page")
(def client-logout-link ".func--logout__link")

(defn input-sign-in-credentials-and-submit []
  (wd/input-text stonecutter-sign-in-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-sign-in-password-input "password")
  (wd/click stonecutter-sign-in-button))

(defn attempt-sign-in []
  (wd/to (str "https://" stonecutter-url "/sign-in"))
  (wait-for-selector stonecutter-sign-in-page-body)
  (input-sign-in-credentials-and-submit))

(defn go-to-delete-account []
  (wd/to (str "https://" stonecutter-url "/delete-account"))
  (wait-for-selector stonecutter-delete-account-page-body))

(defn confirm-delete-account []
  (wd/click stonecutter-delete-account-button)
  (wait-for-selector stonecutter-profile-deleted-page-body))

(defn delete-stale-account []
  (attempt-sign-in)
  (wait-for-selector "body")
  (when-not (re-find #"/sign-in" (wd/current-url))
    (go-to-delete-account)
    (confirm-delete-account)))

(defn logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-again []
  (wd/click client-logout-link)
  (wait-for-selector client-home-page-body)
  (wd/click "button")
  (wait-for-selector stonecutter-authorise-page-body)
  (wd/click stonecutter-authorise-share-profile-button)
  (wait-for-selector client-poll-page-body))

(against-background
  [(before :contents (do (wd/set-driver! {:browser :firefox})
                         (clear-screenshots)
                         (delete-stale-account)))
   (after  :contents (do (wd/quit)))]

  (try
    (fact "can register a stonecutter account"
          ;; Go to home page and get redirected to sign-in
          (wd/to (str "https://" stonecutter-url))
          (wait-for-selector stonecutter-sign-in-page-body)
          (wd/current-url) => (contains "stonecutter.herokuapp.com/sign-in")

          ;; Click through to register page
          (wd/click stonecutter-sign-in-page-register-link)
          (wait-for-selector stonecutter-register-page-body)
          (screenshot "stonecutter_register_page")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/register")

          ;; Enter user details to register
          (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
          (wd/input-text stonecutter-register-password-input "password")
          (wd/input-text stonecutter-register-confirm-password-input "password")
          (wd/click stonecutter-register-create-profile-button)

          ;; View profile created page
          (wait-for-selector stonecutter-profile-created-page-body)
          (screenshot "stonecutter_profile_created_page")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/profile-created"))

    (fact "can sign out of stonecutter"
          (wd/to (str "https://" stonecutter-url "/sign-out"))
          (wait-for-selector stonecutter-sign-in-page-body)
          (wd/current-url) => (contains "stonecutter.herokuapp.com/sign-in"))

    (fact "can go to client page"
          (wd/to "https://stonecutter-client.herokuapp.com")
          (wait-for-selector client-home-page-body)
          (screenshot "client_home_page")
          (wd/current-url) => (contains "stonecutter-client.herokuapp.com/login"))

    (fact "'sign in to vote' redirects to stonecutter"
          (wd/click "button")
          (wait-for-selector stonecutter-sign-in-page-body)
          (screenshot "stonecutter_sign_in")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/sign-in"))

    (fact "can sign in with existing user credentials and redirects to authorisation form page"
          (input-sign-in-credentials-and-submit)
          (wait-for-selector stonecutter-authorise-page-body)
          (screenshot "stonecutter_authorisation_form")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/authorisation"))

    (fact "authorising app redirects to voting page"
          (wd/click stonecutter-authorise-share-profile-button)
          (wait-for-selector client-poll-page-body)
          (screenshot "client_voting_page")
          (wd/current-url) => (contains "stonecutter-client.herokuapp.com/voting")
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "logging out in client app and logging in again will skip sign in page (repeats twice as there was a bug)"
          (logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-again)
          (wd/current-url) => (contains "stonecutter-client.herokuapp.com/voting")
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com")

          (logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-again)
          (wd/current-url) => (contains "stonecutter-client.herokuapp.com/voting")
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "can delete account from stonecutter"
          (go-to-delete-account)
          (screenshot "stonecutter_delete_account")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/delete-account")

          (confirm-delete-account)
          (screenshot "stonecutter_profile-deleted")
          (wd/current-url) => (contains "stonecutter.herokuapp.com/profile-deleted"))

    (catch Exception e
      (screenshot "ERROR"))))
