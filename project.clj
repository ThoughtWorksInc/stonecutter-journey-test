(defproject stonecutter-journey-test "0.1.0-SNAPSHOT"
  :description "A functional test to test interaction between stonecutter and stonecutter-client"
  :test-paths ["test"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [midje "1.6.3"]
                 [xml-apis "1.4.01"]
                 [environ "1.0.0"]
                 [clj-webdriver "0.6.1" :exclusions [org.seleniumhq.selenium/selenium-java
                                                     org.seleniumhq.selenium/selenium-server
                                                     org.seleniumhq.selenium/selenium-remote-driver
                                                     xml-apis]]
                 [org.seleniumhq.selenium/selenium-server "2.45.0"]
                 [org.seleniumhq.selenium/selenium-java "2.45.0"]
                 [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"]]
  :plugins  [[lein-midje "3.1.3"]])
