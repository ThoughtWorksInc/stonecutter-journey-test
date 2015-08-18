(ns stonecutter-journey-test.openid-test
  (:require [midje.sweet :refer :all]
            [environ.core :as env]
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

(def scheme (get env/env :scheme "https"))

(def stonecutter-url (get env/env :stonecutter-url "sso-staging.dcentproject.eu"))
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

(defn input-sign-in-credentials-and-submit []
  (wd/input-text stonecutter-sign-in-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-sign-in-password-input "password")
  (wd/click stonecutter-sign-in-button))

(defn attempt-sign-in []
  (wd/to (str scheme "://" stonecutter-url "/sign-in"))
  (wait-for-selector stonecutter-sign-in-page-body)
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
  (when-not (re-find #"/sign-in" (wd/current-url))
    (go-to-delete-account)
    (confirm-delete-account)))

(defn logout-of-client-and-click-sign-in-to-vote []
  (wd/click client-logout-link)
  (wait-for-selector client-home-page-body)
  (wd/click "button"))

(defn logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again []
  (logout-of-client-and-click-sign-in-to-vote)
  (wait-for-selector client-poll-page-body))

(against-background
  [(before :contents (do (wd/set-driver! {:browser :firefox})
                         (clear-screenshots)
                         (delete-stale-account)))
   (after  :contents (do (wd/quit)))]

  (try
    (fact "can register a stonecutter account"
          ;; Go to home page and get redirected to sign-in
          (wd/to (str scheme "://" stonecutter-url))
          (wait-for-selector stonecutter-sign-in-page-body)
          (wd/current-url) => (contains (str stonecutter-url "/sign-in"))

          ;; Click through to register page
          (wd/click stonecutter-sign-in-page-register-link)
          (wait-for-selector stonecutter-register-page-body)
          (screenshot "openid_stonecutter_register_page")
          (wd/current-url) => (contains (str stonecutter-url "/register"))

          ;; Enter user details to register
          (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
          (wd/input-text stonecutter-register-password-input "password")
          (wd/input-text stonecutter-register-confirm-password-input "password")
          (wd/click stonecutter-register-create-profile-button)

          ;; View profile created page
          (wait-for-selector stonecutter-profile-created-page-body)
          (screenshot "openid_stonecutter_profile_created_page")
          (wd/current-url) => (contains (str stonecutter-url "/profile-created")))

    (fact "can sign out of stonecutter"
          (wd/to (str scheme "://" stonecutter-url "/sign-out"))
          (wait-for-selector stonecutter-sign-in-page-body)
          (wd/current-url) => (contains (str stonecutter-url "/sign-in")))

    (fact "can go to client openid login page"
          (wd/to (str scheme "://" stonecutter-client-url "/openid/login"))
          (wait-for-selector client-home-page-body)
          (screenshot "openid_client_home_page")
          (wd/current-url) => (contains (str stonecutter-client-url "/openid/login")))

    (fact "'sign in to vote' redirects to stonecutter"
          (wd/click "button")
          (wait-for-selector stonecutter-sign-in-page-body)
          (screenshot "openid_stonecutter_sign_in")
          (wd/current-url) => (contains (str stonecutter-url "/sign-in")))

    (fact "can sign in with existing user credentials and redirects to authorisation form page"
          (input-sign-in-credentials-and-submit)
          (wait-for-selector stonecutter-authorise-page-body)
          (screenshot "openid_stonecutter_authorisation_form")
          (wd/current-url) => (contains (str stonecutter-url "/authorisation")))

    (fact "denying app redirects to home page"
          (wd/click stonecutter-authorise-cancel-link)
          (wait-for-selector stonecutter-authorise-failure-body)
          (screenshot "openid_stonecutter_authorise_failure")
          (wd/click stonecutter-authorise-return-to-client-link)
          (wait-for-selector client-home-page-body)
          (wd/current-url) => (contains (str stonecutter-client-url "/"))
          (wd/page-source) =not=> (contains "stonecutter-journey-test@tw.com"))

    (fact "authorising app redirects to voting page"
          (wd/to (str scheme "://" stonecutter-client-url "/openid/login"))
          (wd/click "button")
          (wait-for-selector stonecutter-authorise-page-body)
          (wd/click stonecutter-authorise-share-profile-button)
          (wait-for-selector client-poll-page-body)
          (screenshot "openid_client_voting_page")
          (wd/current-url) => (contains (str stonecutter-client-url "/openid/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "logging out in client app and logging in again will skip sign in page
          and authorise page (repeats twice as there was a bug)"
          (logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again)
          (wd/current-url) => (contains (str stonecutter-client-url "/openid/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com")

          (logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again)
          (wd/current-url) => (contains (str stonecutter-client-url "/openid/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "can unshare profile card and then logging in to client app will require authorising the app again"
          ;; unshare profile card
          (wd/to (str scheme "://" stonecutter-url "/profile"))
          (wait-for-selector stonecutter-profile-page-body)
          (screenshot "openid_stonecutter_profile_with_client_app")
          (wd/click stonecutter-profile-unshare-profile-card-link)
          (wait-for-selector stonecutter-unshare-profile-card-page-body)
          (screenshot "openid_stonecutter_unshare_profile_card")
          (wd/click stonecutter-unshare-profile-card-button)
          (wait-for-selector stonecutter-profile-page-body)
          (screenshot "openid_stonecutter_profile_without_client_app")

          ;; login to client app
          (wd/to (str scheme "://" stonecutter-client-url "/openid/"))
          (logout-of-client-and-click-sign-in-to-vote)
          (wait-for-selector stonecutter-authorise-page-body)
          (screenshot "openid_stonecutter_authorisation_form_again")
          (wd/current-url) => (contains (str stonecutter-url "/authorisation"))

          ;; authorise client app
          (wd/click stonecutter-authorise-share-profile-button)
          (wait-for-selector client-poll-page-body)
          (screenshot "openid_client_voting_page_again")
          (wd/current-url) => (contains (str stonecutter-client-url "/openid/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "can delete account from stonecutter"
          (go-to-delete-account)
          (screenshot "openid_stonecutter_delete_account")
          (wd/current-url) => (contains (str stonecutter-url "/delete-account"))

          (confirm-delete-account)
          (screenshot "openid_stonecutter_profile-deleted")
          (wd/current-url) => (contains (str stonecutter-url "/profile-deleted")))

    (fact "can continue to authorise app even when registering a new account"
          (wd/to (str scheme "://" stonecutter-client-url "/openid/"))
          (logout-of-client-and-click-sign-in-to-vote)
          (wait-for-selector stonecutter-sign-in-page-body)
          (wd/click stonecutter-sign-in-page-register-link)
          (wait-for-selector stonecutter-register-page-body)

          ;; Enter user details to register
          (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
          (wd/input-text stonecutter-register-password-input "password")
          (wd/input-text stonecutter-register-confirm-password-input "password")
          (wd/click stonecutter-register-create-profile-button)

          ;; View profile created page
          (wait-for-selector stonecutter-profile-created-page-body)
          (screenshot "openid_stonecutter_profile_created_page_from_auth")

          (wd/click stonecutter-profile-created-next-link)
          (wait-for-selector stonecutter-authorise-page-body)

          (screenshot "openid_stonecutter_authorisation_form_after_registration")
          (wd/current-url) => (contains (str stonecutter-url "/authorisation"))

          ;; Cleaning up
          (go-to-delete-account)
          (wd/current-url) => (contains (str stonecutter-url "/delete-account"))
          (confirm-delete-account)
          (wd/current-url) => (contains (str stonecutter-url "/profile-deleted")))

    (catch Exception e
      (screenshot "ERROR")
      (throw e))))
