(ns stonecutter-journey-test.common
  (:require
    [environ.core :as env]
    [clj-webdriver.taxi :as wd]
    [clojure.java.io :as io]))

(def stonecutter-url (get env/env :stonecutter-url "sso-staging.dcentproject.eu"))
(def stonecutter-index-page-body ".func--index-page")
(def stonecutter-sign-in-email-input ".func--sign-in-email__input")
(def stonecutter-sign-in-password-input ".func--sign-in-password__input")
(def stonecutter-sign-in-button ".func--sign-in__button")
(def stonecutter-register-email-input ".func--registration-email__input")
(def stonecutter-register-password-input ".func--registration-password__input")
(def stonecutter-register-confirm-password-input ".func--registration-confirm-password__input")
(def stonecutter-register-create-profile-button ".func--create-profile__button")
(def stonecutter-profile-page-body ".func--profile-page")
(def stonecutter-profile-unshare-profile-card-link ".func--app-item__unshare-link")
(def stonecutter-profile-created-page-body ".func--profile-created-page")
(def stonecutter-profile-created-next-link ".func--profile-created-next__button")
(def stonecutter-profile-deleted-page-body ".func--profile-deleted-page")
(def stonecutter-unshare-profile-card-page-body ".func--unshare-profile-card-page")
(def stonecutter-unshare-profile-card-button ".func--unshare-profile-card__button")
(def stonecutter-authorise-page-body ".func--authorise-page")
(def stonecutter-authorise-failure-body ".func--authorise-failure-page")
(def stonecutter-authorise-cancel-link ".func--authorise-cancel__link")
(def stonecutter-authorise-return-to-client-link ".func--redirect-to-client-home__link")
(def stonecutter-authorise-share-profile-button ".func--authorise-share-profile__button")
(def stonecutter-delete-account-page-body ".func--delete-account-page")
(def stonecutter-delete-account-button ".func--delete-account__button")


(def stonecutter-client-url (get env/env :stonecutter-client-url "stonecutter-client.herokuapp.com"))
(def client-home-page-body ".func--home-page")
(def client-poll-page-body ".func--poll-page")
(def client-logout-link ".func--logout__link")

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

(def scheme (get env/env :scheme "https"))

(defn input-sign-in-credentials-and-submit []
  (wd/input-text stonecutter-sign-in-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-sign-in-password-input "password")
  (wd/click stonecutter-sign-in-button))

(defn register-and-sign-out []
  (wd/to (str scheme "://" stonecutter-url))
  (wait-for-selector stonecutter-index-page-body)
  (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-register-password-input "password")
  (wd/input-text stonecutter-register-confirm-password-input "password")
  (wd/click stonecutter-register-create-profile-button)
  (wait-for-selector stonecutter-profile-created-page-body)
  (wd/to (str scheme "://" stonecutter-url "/sign-out"))
  (wait-for-selector stonecutter-index-page-body))

(defn attempt-sign-in []
  (wd/to (str scheme "://" stonecutter-url "/"))
  (wait-for-selector stonecutter-index-page-body)
  (input-sign-in-credentials-and-submit))

(defn go-to-delete-account []
  (wd/to (str scheme "://" stonecutter-url "/delete-account"))
  (wait-for-selector stonecutter-delete-account-page-body))

(defn confirm-delete-account []
  (wd/click stonecutter-delete-account-button)
  (wait-for-selector stonecutter-profile-deleted-page-body))

(defn delete-stale-account []
  (attempt-sign-in)
  (wait-for-selector "body")
  (when (re-find #"/profile" (wd/current-url))
    (go-to-delete-account)
    (confirm-delete-account)))

(defn logout-of-client-and-click-sign-in-to-vote []
  (wd/click client-logout-link)
  (wait-for-selector client-home-page-body)
  (wd/click "button"))

(defn logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again []
  (logout-of-client-and-click-sign-in-to-vote)
  (wait-for-selector client-poll-page-body))

