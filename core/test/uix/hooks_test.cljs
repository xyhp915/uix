(ns uix.hooks-test
  (:require [clojure.test :refer [deftest is testing run-tests async]]
            [uix.core :as uix]
            ["@testing-library/react" :as rtl]
            [uix.hooks.alpha :as hooks]))

(defn render-hook [f]
  (.. (rtl/renderHook f) -result -current))

(deftest test-with-return-value-check
  (testing "should return js/undefined when return value is not a function"
    (let [f (hooks/with-return-value-check (constantly 1))]
      (is (= js/undefined (f)))))
  (testing "when return value is a function, should return the function"
    (let [f (hooks/with-return-value-check (constantly identity))]
      (is (= identity (f))))))

(deftest test-use-state
  (let [[v f] (render-hook #(uix/use-state 0))]
    (is (zero? v))))

(deftest test-use-effect
  (let [v (render-hook (fn []
                         (let [[v f] (uix/use-state 0)]
                           (uix/use-effect #(f inc) [])
                           v)))]
    (is (= 1 v))))

(defn -main []
  (run-tests))

