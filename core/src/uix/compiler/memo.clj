(ns uix.compiler.memo
  (:require [uix.hooks.linter :as hooks.linter])
  (:import (cljs.tagged_literals JSValue)))

(def dynamic-val?
  ;; a dynamic value is either a symbol or an expression
  (some-fn symbol? #(and (seq? %) (not= 'quote (first %)))))

(defn find-props-deps [m]
  (let [bindings (atom [])
        ret (clojure.walk/prewalk
             #(cond
                (dynamic-val? %)
                (let [sym (gensym "uix-prop")]
                  (swap! bindings conj [sym %])
                  sym)

                (= (type %) JSValue)
                (.-val %)

                :else %)
             m)]
    [ret @bindings]))

(defn props->props+deps [props-map]
  (let [[props-map bindings] (find-props-deps props-map)
        props-deps (map first bindings)]
    [bindings props-deps props-map]))

(defn children->children+deps [children]
  (let [children-bindings (map #(vector (gensym "uix-child") %) children)
        child-deps (map first children-bindings)]
    [children-bindings child-deps]))

(defn memoize-fn-bindings
  "Wraps anonymous functions in `uix.core/use-callback`
  with an inferred vector of deps"
  [env bindings]
  (for [[sym form] bindings]
    (if (hooks.linter/fn-literal? form)
      [sym `(uix.core/use-callback ~form ~(vec (hooks.linter/find-local-variables env form)))]
      [sym form])))

(defn memoize-element* [env props children create-el]
  ;; React element can be memoized on two types of values present at element declaration place:
  ;; 1. "dynamic" expressions: symbols and function calls `(...)`
  ;; 3. Child elements
  ;; Here's the algo
  ;; 1. Both dynamic exprs and child elements are hoisted into local bindings
  ;; 2. Local bindings form a list of deps for memoized React element
  ;; 3. Every UIx component has a single local cache for memoized elements in a form of `use-ref` (see *memo-cache*)
  ;; 4. When a component is rendered, every element is created and cached on its deps with a unique ID
  (let [[props-bindings props-deps props] (props->props+deps props)
        props-bindings (memoize-fn-bindings env props-bindings)
        [children-bindings child-deps] (children->children+deps children)
        bindings (concat props-bindings children-bindings)
        deps (vec (concat props-deps child-deps))
        id (str (gensym "id"))]
    `(let [~@(mapcat identity bindings)]
       (uix.compiler.aot/use-memo-cache (fn [] ~(create-el props child-deps)) ~id ~deps))))

(defn memoize-element [env attrs children create-el]
  (if (map? attrs)
    ;; memoizing an element only when it's known to have a map of props
    (memoize-element* env attrs children create-el)
    (create-el attrs children)))
