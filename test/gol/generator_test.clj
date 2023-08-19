(ns gol.generator-test 
  (:require [clojure.test :as t :refer [is testing]]
            [gol.generator :as generator]
            [dev.meta :as dev]))


(t/deftest generator-test
  (let [gen (generator/generator 0 inc)]
    (testing "simple increment"
      (is (= 0 (gen)))
      (is (= 1 (gen))))
    (testing "initial value"
      (is (= 5 (gen 5)))
      (is (= 5 (gen)))
      (testing "with update"
        (is (= 6 (gen)))))))

(dev.meta/annotate)