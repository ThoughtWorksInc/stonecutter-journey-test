(defproject stonecutter-journey-test "0.1.0-SNAPSHOT"
  :description "A functional test to test interaction between stonecutter and stonecutter-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot stonecutter-webdriver.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
