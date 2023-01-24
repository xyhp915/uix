(ns uix.hooks-test
  (:require [clojure.test :refer [deftest is testing run-tests async]]
            [uix.hooks.alpha :as hooks]))

(deftest test-with-return-value-check
  (testing "should return js/undefined when return value is not a function"
    (let [f (hooks/with-return-value-check (constantly 1))]
      (is (= js/undefined (f)))))
  (testing "when return value is a function, should return the function"
    (let [f (hooks/with-return-value-check (constantly identity))]
      (is (= identity (f))))))

(defn -main []
  (run-tests))

