(ns uix.re-frame-test
  (:require [clojure.test :refer [deftest is testing]]
            ["@testing-library/react" :as rtl]
            [reagent.core :as r]
            [uix.re-frame :refer [use-reaction use-subscribe]]))

(deftest test-use-reaction
  (let [ref (r/atom 0)
        result (rtl/renderHook #(use-reaction ref))
        rerender (.-rerender result)
        unmount (.-unmount result)
        result (.-result result)]
    (testing "should track ratom value"
      (is (= 0 (.-current result)))
      (swap! ref inc)
      (rerender)
      (is (= 1 (.-current result))))
    (testing "should cleanup listeners on unmount"
      (unmount)
      (is (nil? (.-react-listeners ref)))
      (is (empty? (.-watches ref))))
    (testing "should continue tracking the same ratom"
      (let [result (rtl/renderHook #(use-reaction ref))
            rerender (.-rerender result)
            result (.-result result)]
        (is (= 1 (.-current result)))
        (swap! ref inc)
        (rerender)
        (is (= 2 (.-current result)))))))