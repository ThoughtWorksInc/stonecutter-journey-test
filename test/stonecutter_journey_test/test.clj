(ns stonecutter-journey-test.test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [clj-webdriver.core :as wc]))

(fact "can go to client page"
      (wd/set-driver! {:browser :firefox})
      (wd/to "https://stonecutter-client.herokuapp.com")
      (wd/current-url) => "https://stonecutter-client.herokuapp.com/")
