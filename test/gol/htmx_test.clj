(ns gol.htmx-test 
  (:require [clojure.test :refer [deftest is testing]]
            [dev.meta :as dev]))

(deftest htmx-test
   (testing "Context of the test assertions"
     (is (= 1 1))))

(dev/annotate)

