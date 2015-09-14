(ns stonecutter-journey-test.oauth-test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [stonecutter-journey-test.common :as c]))

(against-background
  [(before :contents (do (wd/set-driver! {:browser :firefox})
                         (c/clear-screenshots)
                         (c/delete-stale-account)))
   (after  :contents (do (wd/quit)))]

  (try
    (fact "can register a stonecutter account"
          ;; Go to home page and get redirected to sign-in
          (wd/to (str c/scheme "://" c/stonecutter-url))
          (c/wait-for-selector c/stonecutter-index-page-body)
          (c/screenshot "stonecutter_index_page")
          (wd/current-url) => (contains (str c/stonecutter-url "/"))

          ;; Enter user details to register
          (wd/input-text c/stonecutter-register-email-input "stonecutter-journey-test@tw.com")
          (wd/input-text c/stonecutter-register-password-input "password")
          (wd/click c/stonecutter-register-create-profile-button)

          ;; View profile created page
          (c/wait-for-selector c/stonecutter-profile-created-page-body)
          (c/screenshot "stonecutter_profile_created_page")
          (wd/current-url) => (contains (str c/stonecutter-url "/profile-created")))

    (fact "can sign out of stonecutter"
          (wd/to (str c/scheme "://" c/stonecutter-url "/sign-out"))
          (c/wait-for-selector c/stonecutter-index-page-body)
          (wd/current-url) => (contains (str c/stonecutter-url "/")))

    (fact "can go to client page"
          (wd/to (str c/scheme "://" c/stonecutter-client-url))
          (c/wait-for-selector c/client-home-page-body)
          (c/screenshot "client_home_page")
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/login")))

    (fact "'sign in to vote' redirects to stonecutter"
          (wd/click "button")
          (c/wait-for-selector c/stonecutter-index-page-body)
          (wd/current-url) => (contains (str c/stonecutter-url "/")))

    (fact "can sign in with existing user credentials and redirects to authorisation form page"
          (c/input-sign-in-credentials-and-submit)
          (c/wait-for-selector c/stonecutter-authorise-page-body)
          (c/screenshot "stonecutter_authorisation_form")
          (wd/current-url) => (contains (str c/stonecutter-url "/authorisation")))

    (fact "denying app redirects to home page"
          (wd/click c/stonecutter-authorise-cancel-link)
          (c/wait-for-selector c/stonecutter-authorise-failure-body)
          (c/screenshot "stonecutter_authorise_failure")
          (wd/click c/stonecutter-authorise-return-to-client-link)
          (c/wait-for-selector c/client-home-page-body)
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/login"))
          (wd/page-source) =not=> (contains "stonecutter-journey-test@tw.com"))

    (fact "authorising app redirects to voting page"
          (wd/click "button")
          (c/wait-for-selector c/stonecutter-authorise-page-body)
          (wd/click c/stonecutter-authorise-share-profile-button)
          (c/wait-for-selector c/client-poll-page-body)
          (c/screenshot "client_voting_page")
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "logging out in client app and logging in again will skip sign in page
          and authorise page (repeats twice as there was a bug)"
          (c/logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again)
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com")

          (c/logout-of-client-and-go-through-auth-flow-again-without-having-to-sign-in-or-authorise-app-again)
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "can unshare profile card and then logging in to client app will require authorising the app again"
          ;; unshare profile card
          (wd/to (str c/scheme "://" c/stonecutter-url "/profile"))
          (c/wait-for-selector c/stonecutter-profile-page-body)
          (c/screenshot "stonecutter_profile_with_client_app")
          (wd/click c/stonecutter-profile-unshare-profile-card-link)
          (c/wait-for-selector c/stonecutter-unshare-profile-card-page-body)
          (c/screenshot "stonecutter_unshare_profile_card")
          (wd/click c/stonecutter-unshare-profile-card-button)
          (c/wait-for-selector c/stonecutter-profile-page-body)
          (c/screenshot "stonecutter_profile_without_client_app")

          ;; login to client app
          (wd/to (str c/scheme "://" c/stonecutter-client-url))
          (c/logout-of-client-and-click-sign-in-to-vote)
          (c/wait-for-selector c/stonecutter-authorise-page-body)
          (c/screenshot "stonecutter_authorisation_form_again")
          (wd/current-url) => (contains (str c/stonecutter-url "/authorisation"))

          ;; authorise client app
          (wd/click c/stonecutter-authorise-share-profile-button)
          (c/wait-for-selector c/client-poll-page-body)
          (c/screenshot "client_voting_page_again")
          (wd/current-url) => (contains (str c/stonecutter-client-url "/oauth/voting"))
          (wd/page-source) => (contains "stonecutter-journey-test@tw.com"))

    (fact "can delete account from stonecutter"
          (c/go-to-delete-account)
          (c/screenshot "stonecutter_delete_account")
          (wd/current-url) => (contains (str c/stonecutter-url "/delete-account"))

          (c/confirm-delete-account)
          (c/screenshot "stonecutter_profile-deleted")
          (wd/current-url) => (contains (str c/stonecutter-url "/profile-deleted")))

    (fact "can continue to authorise app even when registering a new account"
          (wd/to (str c/scheme "://" c/stonecutter-client-url))
          (c/logout-of-client-and-click-sign-in-to-vote)
          (c/wait-for-selector c/stonecutter-index-page-body)

          ;; Enter user details to register
          (wd/input-text c/stonecutter-register-email-input "stonecutter-journey-test@tw.com")
          (wd/input-text c/stonecutter-register-password-input "password")
          (wd/click c/stonecutter-register-create-profile-button)

          ;; View profile created page
          (c/wait-for-selector c/stonecutter-profile-created-page-body)
          (c/screenshot "stonecutter_profile_created_page_from_auth")

          (wd/click c/stonecutter-profile-created-next-link)
          (c/wait-for-selector c/stonecutter-authorise-page-body)

          (c/screenshot "stonecutter_authorisation_form_after_registration")
          (wd/current-url) => (contains (str c/stonecutter-url "/authorisation"))

          ;; Cleaning up
          (c/go-to-delete-account)
          (wd/current-url) => (contains (str c/stonecutter-url "/delete-account"))
          (c/confirm-delete-account)
          (wd/current-url) => (contains (str c/stonecutter-url "/profile-deleted")))

    (catch Exception e
      (c/screenshot "ERROR")
      (throw e))))
