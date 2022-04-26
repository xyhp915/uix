(ns uix.compiler.memo
  (:require [uix.hooks.linter :as hooks.linter]
            [clojure.set :as set])
  (:import (cljs.tagged_literals JSValue)))

(defn find-local-hooks [f]
  (let [bindings (atom [])
        form (clojure.walk/postwalk
               #(cond
                  (hooks.linter/hook-call? %)
                  (let [sym (gensym "uix-hook")]
                    (swap! bindings conj [sym %])
                    sym)

                  (= (type %) JSValue)
                  (.-val %)

                  :else %)
               f)]
    [form @bindings]))

(defn find-props-deps [env m]
  (let [rmap (->> (seq m)
                  (filter #(seq (hooks.linter/find-local-variables env (val %)))))
        props-bindings (map #(vector (gensym "uix-prop") (val %)) rmap)
        props-deps (map first props-bindings)
        deps-m (zipmap (map key rmap) (map first props-bindings))
        m (into m deps-m)]
    [props-bindings props-deps m]))

(defn memoize-element [env form attrs children create-el]
  ;; React element can be memoized on three types of values present at element declaration place:
  ;; 1. Referenced local vars
  ;; 2. Hook calls
  ;; 3. Child elements
  ;; Here's the algo
  ;; 1. Both hook calls and child elements are hoisted into local bindings
  ;; 2. Local bindings are merged with referenced local vars, forming a list of deps for memoized React element
  ;; 3. Every UIx component has a single local cache for memoized elements in a form of `use-ref` (see *memo-cache*)
  ;; 4. When a component is rendered, every element is created and cached on its deps with a unique ID
  (if-not (map? attrs)
    (create-el attrs children)
    (let [[props-bindings props-deps attrs] (find-props-deps env attrs)

          [attrs hooks-bindings] (find-local-hooks attrs)
          hooks-deps (map first hooks-bindings)

          children-bindings (map #(vector (gensym "uix-child") %) children)
          child-deps (map first children-bindings)

          bindings (concat props-bindings hooks-bindings children-bindings)
          deps (-> (into [] props-deps)
                   (into hooks-deps)
                   (into child-deps))
          id `(str ~(str (gensym "id")) ~(:key attrs))]
      `(let [~@(mapcat identity bindings)]
         (uix.compiler.aot/use-memo-cache (fn [] ~(create-el attrs child-deps)) ~id ~deps)))))
