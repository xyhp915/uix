(ns uix.core
  "Public API"
  (:refer-clojure :exclude [fn])
  (:require [clojure.core :as core]
            [clojure.string :as str]
            [uix.compiler.aot :as aot]
            [uix.source]
            [cljs.core]
            [uix.linter]
            [uix.dev]
            [uix.lib]
            [uix.hooks.alpha :as hooks]))

(def ^:private goog-debug (with-meta 'goog.DEBUG {:tag 'boolean}))

(defn- no-args-component [sym var-sym body]
  `(defn ~sym []
     (let [f# (core/fn [] ~@body)]
       (if ~goog-debug
         (binding [*current-component* ~var-sym] (f#))
         (f#)))))

(defn- with-args-component [sym var-sym args body]
  `(defn ~sym [props#]
     (let [clj-props# (glue-args props#)
           ~args (cljs.core/array clj-props#)
           f# (core/fn [] ~@body)]
       (if ~goog-debug
         (binding [*current-component* ~var-sym]
           (assert (or (map? clj-props#)
                       (nil? clj-props#))
                   (str "UIx component expects a map of props, but instead got " clj-props#))
           (f#))
         (f#)))))

(defn- no-args-fn-component [sym var-sym body]
  `(core/fn ~sym []
     (let [f# (core/fn [] ~@body)]
       (if ~goog-debug
         (binding [*current-component* ~var-sym] (f#))
         (f#)))))

(defn- with-args-fn-component [sym var-sym args body]
  `(core/fn ~sym [props#]
     (let [clj-props# (glue-args props#)
           ~args (cljs.core/array clj-props#)
           f# (core/fn [] ~@body)]
       (if ~goog-debug
         (binding [*current-component* ~var-sym]
           (assert (or (map? clj-props#)
                       (nil? clj-props#))
                   (str "UIx component expects a map of props, but instead got " clj-props#))
           (f#))
         (f#)))))

(defn parse-defui-sig [form name fdecl]
  (let [[fname fdecl] (uix.lib/parse-sig name fdecl)]
    (uix.lib/assert!
     (= 1 (count fdecl))
     (str form " doesn't support multi-arity.\n"
          "If you meant to make props an optional argument, you can safely skip it and have a single-arity component.\n
                 It's safe to destructure the props value even if it's `nil`."))
    (let [[args & fdecl] (first fdecl)]
      (uix.lib/assert!
       (>= 1 (count args))
       (str form " is a single argument component taking a map of props, found: " args "\n"
            "If you meant to retrieve `children`, they are under `:children` field in props map."))
      [fname args fdecl])))

(defn- set-display-name [f name]
  `(do
     (set! (.-displayName ~f) ~name)
     (js/Object.defineProperty ~f "name" (cljs.core/js-obj "value" ~name))))

(defmacro
  ^{:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])}
  defui
  "Creates UIx component. Similar to defn, but doesn't support multi arity.
  A component should have a single argument of props."
  [sym & fdecl]
  (let [[fname args fdecl] (parse-defui-sig `defui sym fdecl)]
    (uix.linter/lint! sym fdecl &form &env)
    (if (uix.lib/cljs-env? &env)
      (let [memo? (-> sym meta :memo)
            memo-sym (gensym fname)
            memo-fname (if memo?
                         (with-meta memo-sym (meta fname))
                         fname)
            var-sym (-> (str (-> &env :ns :name) "/" fname) symbol (with-meta {:tag 'js}))
            memo-var-sym (-> (str (-> &env :ns :name) "/" memo-fname) symbol (with-meta {:tag 'js}))
            body (uix.dev/with-fast-refresh memo-var-sym fdecl)
            body (aot/rewrite-forms body)]
        `(do
           ~(if (empty? args)
              (no-args-component memo-fname memo-var-sym body)
              (with-args-component memo-fname memo-var-sym args body))
           (set! (.-uix-component? ~memo-var-sym) true)
           ~(set-display-name memo-var-sym (str var-sym))
           ~(uix.dev/fast-refresh-signature memo-var-sym body)
           ~(when memo?
              `(def ~fname (uix.core/memo ~memo-sym)))))
      `(defn ~fname [& args#]
         (let [~args args#]
           ~@fdecl)))))

(defmacro fn
  "Creates anonymous UIx component. Similar to fn, but doesn't support multi arity.
  A component should have a single argument of props."
  [& fdecl]
  (let [[sym fdecl] (if (symbol? (first fdecl))
                      [(first fdecl) (rest fdecl)]
                      [(gensym "uix-fn") fdecl])
        [fname args body] (parse-defui-sig `fn sym fdecl)
        body (aot/rewrite-forms body)]
    (uix.linter/lint! sym body &form &env)
    (if (uix.lib/cljs-env? &env)
      (let [var-sym (with-meta sym {:tag 'js})]
        `(let [~var-sym ~(if (empty? args)
                           (no-args-fn-component fname var-sym body)
                           (with-args-fn-component fname var-sym args body))]
           (set! (.-uix-component? ~var-sym) true)
           ~(set-display-name var-sym (str var-sym))
           ~var-sym))
      `(core/fn ~fname [& args#]
         (let [~args args#]
           ~@fdecl)))))

(defmacro source
  "Returns source string of UIx component"
  [sym]
  (uix.source/source &env sym))

(defmacro $
  "Creates React element

  DOM element: ($ :button#id.class {:on-click handle-click} \"click me\")
  React component: ($ title-bar {:title \"Title\"})"
  ([tag]
   (uix.linter/lint-element* &form &env)
   (uix.compiler.aot/compile-element [tag] {:env &env}))
  ([tag props & children]
   (uix.linter/lint-element* &form &env)
   (uix.compiler.aot/compile-element (into [tag props] children) {:env &env})))

(defn parse-defhook-sig [sym fdecl]
  (let [[fname fdecl] (uix.lib/parse-sig sym fdecl)]
    (uix.lib/assert! (str/starts-with? (name fname) "use-")
                     (str "React Hook name should start with `use-`, found `" (name fname) "` instead."))
    (uix.lib/assert!
     (= 1 (count fdecl))
     "uix.core/defhook should be single-arity function")
    [fname fdecl]))

(defmacro
  ^{:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])}
  defhook
  "Like `defn`, but creates React hook with additional validation,
  the name should start with `use-`
  (defhook use-in-viewport []
    ...)"
  [sym & fdecl]
  (let [[fname fdecl] (parse-defhook-sig sym fdecl)
        fname (vary-meta fname assoc :uix/hook true)]
    (uix.linter/lint! sym fdecl &form &env)
    `(defn ~fname ~@fdecl)))

;; === Error boundary ===

(defn create-error-boundary
  "Creates React's error boundary component

  display-name       — the name of the component to be displayed in stack trace
  derive-error-state — maps error object to component's state that is used in render-fn
  did-catch          — 2 arg function for side effeects, logging etc.
  receives the exception and additional component info as args
  render-fn          — takes state value returned from derive-error-state and a vector
  of arguments passed into error boundary"
  [{:keys [display-name derive-error-state did-catch]
    :or   {display-name (str (gensym "uix.error-boundary"))}}
   render-fn]
  ^::error-boundary {:display-name       display-name
                     :render-fn          render-fn
                     :did-catch          did-catch
                     :derive-error-state derive-error-state})

;; === Hooks ===

(defn use-state [value]
  (hooks/use-state value))

(defn use-reducer
  ([f value]
   (hooks/use-reducer f value))
  ([f value init-state]
   (hooks/use-reducer f value init-state)))

(defn use-ref [value]
  (hooks/use-ref value))

(defn use-context [value]
  (hooks/use-context value))

(defn use-debug
  ([v]
   (hooks/use-debug v))
  ([v fmt]
   (hooks/use-debug v fmt)))

(defn use-deferred-value [v]
  (hooks/use-deferred-value v))

(defn use-transition []
  (hooks/use-transition))

(defn use-id []
  (hooks/use-id))

(defn use-sync-external-store
  ([subscribe get-snapshot]
   (hooks/use-sync-external-store subscribe get-snapshot))
  ([subscribe get-snapshot get-server-snapshot]
   (hooks/use-sync-external-store subscribe get-snapshot get-server-snapshot)))

(defn ->js-deps [coll]
  `(cljs.core/array (uix.hooks.alpha/use-clj-deps ~coll)))

(defn- make-hook-with-deps [sym env form f deps]
  (when (uix.lib/cljs-env? env)
    (uix.linter/lint-exhaustive-deps! env form f deps))
  (if deps
    (if (uix.lib/cljs-env? env)
      `(~sym ~f ~(->js-deps deps))
      `(~sym ~f ~deps))
    `(~sym ~f)))

(defmacro use-effect
  "Takes a function to be executed in an effect and optional vector of dependencies.

  See: https://reactjs.org/docs/hooks-reference.html#useeffect"
  ([f]
   (make-hook-with-deps 'uix.hooks.alpha/use-effect &env &form f nil))
  ([f deps]
   (make-hook-with-deps 'uix.hooks.alpha/use-effect &env &form f deps)))

(defmacro use-layout-effect
  "Takes a function to be executed in a layout effect and optional vector of dependencies.

  See: https://reactjs.org/docs/hooks-reference.html#uselayouteffect"
  ([f]
   (make-hook-with-deps 'uix.hooks.alpha/use-layout-effect &env &form f nil))
  ([f deps]
   (make-hook-with-deps 'uix.hooks.alpha/use-layout-effect &env &form f deps)))

(defmacro use-insertion-effect
  "Takes a function to be executed synchronously before all DOM mutations
  and optional vector of dependencies. Use this to inject styles into the DOM
  before reading layout in `useLayoutEffect`.

  See: https://reactjs.org/docs/hooks-reference.html#useinsertioneffect"
  ([f]
   (make-hook-with-deps 'uix.hooks.alpha/use-insertion-effect &env &form f nil))
  ([f deps]
   (make-hook-with-deps 'uix.hooks.alpha/use-insertion-effect &env &form f deps)))

(defmacro use-memo
  "Takes function f and required vector of dependencies, and returns memoized result of f.

   See: https://reactjs.org/docs/hooks-reference.html#usememo"
  [f deps]
  (make-hook-with-deps 'uix.hooks.alpha/use-memo &env &form f deps))

(defmacro use-callback
  "Takes function f and required vector of dependencies, and returns memoized f.

  See: https://reactjs.org/docs/hooks-reference.html#usecallback"
  [f deps]
  (make-hook-with-deps 'uix.hooks.alpha/use-callback &env &form f deps))

(defn use-effect-event
  "EXPERIMENTAL: Creates a stable event handler from a function, allowing it to be used in use-effect
   without adding the function as a dependency.
  See: https://react.dev/learn/separating-events-from-effects"
  [f]
  f)

(defmacro use-imperative-handle
  "Customizes the instance value that is exposed to parent components when using ref.

  See: https://reactjs.org/docs/hooks-reference.html#useimperativehandle"
  ([ref f]
   (when (uix.lib/cljs-env? &env)
     (uix.linter/lint-exhaustive-deps! &env &form f nil))
   `(uix.hooks.alpha/use-imperative-handle ~ref ~f))
  ([ref f deps]
   (when (uix.lib/cljs-env? &env)
     (uix.linter/lint-exhaustive-deps! &env &form f deps))
   `(uix.hooks.alpha/use-imperative-handle ~ref ~f ~(->js-deps deps))))

(defui suspense [{:keys [fallback children]}]
  [::suspense fallback children])

(defui strict-mode [{:keys [children]}]
  children)

(defui profiler [{:keys [children]}]
  children)

(defn clone-element [[type oprops :as element] props & children]
  ($ type (cond-> (if (map? oprops) (merge oprops props) props)
                  (seq children) (assoc :children children))))

;; SSR helpers
(def client? false) ;; no JVM front-ends
(def server? (not client?))
