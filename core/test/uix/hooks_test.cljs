(ns uix.hooks-test
  (:require [clojure.test :refer [deftest is testing run-tests async]]
            [uix.core :refer [defui $]]
            [uix.dom]
            ["react-dom/test-utils" :as rdom.test]))

(defn simulate [event node]
  (let [f (aget rdom.test/Simulate (name event))]
    (f node)))

(defn act [f]
  (rdom.test/act #(let [ret (f)]
                    (if (instance? js/Promise ret) ret js/undefined))))

(defn with-dom-node [f]
  (let [node (.createElement js/document "div")]
    (js/document.body.appendChild node)
    (f node)
    (uix.dom/unmount-at-node node)
    (.remove node)))

(defui test-use-state-comp []
  (let [[v set-v] (uix.core/use-state 0)]
    ($ :button {:on-click #(set-v inc)}
       v)))

(defui test-use-state-updater-comp []
  (let [[v set-v] (uix.core/use-state {:n 0})]
    ($ :button {:on-click #(set-v update :n inc)}
       (:n v))))

(deftest test-use-state
  (testing "simple set-state"
    (with-dom-node
      (fn [^js node]
        (act #(uix.dom/render ($ test-use-state-comp) node))
        (is (= "0" (.-textContent node)))
        (simulate :click (first (.-children node)))
        (is (= "1" (.-textContent node))))))
  (testing "simple swap-like set-state"
    (with-dom-node
      (fn [^js node]
        (act #(uix.dom/render ($ test-use-state-updater-comp) node))
        (is (= "0" (.-textContent node)))
        (simulate :click (first (.-children node)))
        (is (= "1" (.-textContent node)))))))

(defn -main []
  (run-tests))

