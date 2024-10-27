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

(deftest test-use-clj-deps
  (let [ov [1 2 3]
        result (rtl/renderHook
                 #(hooks/use-clj-deps %)
                 #js {:initialProps ov})
        rerender (.-rerender result)
        result (.-result result)
        v (.-current result)
        _ (is (identical? v ov))
        _ (rerender ov)
        v (.-current result)
        _ (is (identical? v ov))
        _ (rerender {:k :v})
        v (.-current result)
        _ (is (= v {:k :v}))]))

(deftest test-use-state
  (let [[v f] (render-hook #(uix/use-state 0))]
    (is (zero? v)))
  (testing "swap! like updater"
    (let [result (rtl/renderHook #(uix/use-state {:n 0}))
          rerender (.-rerender result)
          result (.-result result)
          f (second (.-current result))]
      (f update :n inc)
      (rerender)
      (is (= {:n 1} (first (.-current result)))))))

(deftest test-use-effect
  (let [v (render-hook (fn []
                         (let [[v f] (uix/use-state 0)]
                           (uix/use-effect #(f inc) [])
                           v)))]
    (is (= 1 v)))
  (testing "clj dep"
    (let [result (rtl/renderHook
                   (fn [dep]
                     (let [[v f] (uix/use-state 0)]
                       (uix/use-effect #(f inc) [dep])
                       v))
                   #js {:initialProps {:k :v}})
          rerender (.-rerender result)
          result (.-result result)]
      (is (= 1 (.-current result)))
      (rerender {:k :v})
      (is (= 1 (.-current result)))
      (rerender {:k :vv})
      (is (= 2 (.-current result))))))

(deftest test-clojure-values-identity-use-state
  (testing "UUID: should preserve identity"
    (let [uuid1 #uuid "b137e2ea-a419-464f-8da3-7159005afa35"
          uuid2 (render-hook (fn []
                               (let [[v f] (uix/use-state uuid1)]
                                 (uix/use-effect #(f #uuid "b137e2ea-a419-464f-8da3-7159005afa35") [])
                                 v)))]
      (is (identical? uuid1 uuid2))))
  (testing "Keyword: should preserve identity"
    (let [kw1 :hello/world
          kw2 (render-hook (fn []
                             (let [[v f] (uix/use-state kw1)]
                               (uix/use-effect #(f (constantly :hello/world)) [])
                               v)))]
      (is (identical? kw1 kw2))))
  (testing "Symbol: should preserve identity"
    (let [sym1 'hello-world
          sym2 (render-hook (fn []
                              (let [[v f] (uix/use-state sym1)]
                                (uix/use-effect #(f (constantly 'hello-world)) [])
                                v)))]
      (is (identical? sym1 sym2))))
  (testing "Map: should preserve identity"
    (let [m1 {:hello 'world}
          m2 (render-hook (fn []
                            (let [[v f] (uix/use-state m1)]
                              (uix/use-effect #(f {:hello 'world}) [])
                              v)))]
      (is (identical? m1 m2)))))

(deftest test-clojure-values-identity-use-reducer
  (testing "UUID: should preserve identity"
    (let [uuid1 #uuid "b137e2ea-a419-464f-8da3-7159005afa35"
          uuid2 (render-hook (fn []
                               (let [[v f] (uix/use-reducer (fn [state action] action) uuid1)]
                                 (uix/use-effect #(f #uuid "b137e2ea-a419-464f-8da3-7159005afa35") [])
                                 v)))]
      (is (identical? uuid1 uuid2))))
  (testing "Keyword: should preserve identity"
    (let [kw1 :hello/world
          kw2 (render-hook (fn []
                             (let [[v f] (uix/use-reducer (fn [state action] action) kw1)]
                               (uix/use-effect #(f :hello/world) [])
                               v)))]
      (is (identical? kw1 kw2))))
  (testing "Symbol: should preserve identity"
    (let [sym1 'hello-world
          sym2 (render-hook (fn []
                              (let [[v f] (uix/use-reducer (fn [state action] action) sym1)]
                                (uix/use-effect #(f 'hello-world) [])
                                v)))]
      (is (identical? sym1 sym2))))
  (testing "Map: should preserve identity"
    (let [m1 {:hello 'world}
          m2 (render-hook (fn []
                            (let [[v f] (uix/use-reducer (fn [state action] action) m1)]
                              (uix/use-effect #(f {:hello 'world}) [])
                              v)))]
      (is (identical? m1 m2)))))

