(ns uix.hooks.alpha
  "Wrappers for React Hooks"
  (:require [react :as r]))

(defn- choose-value [nv cv]
  (if (= nv cv)
    cv
    nv))

(defn- use-clojure-aware-updater
  "Replicates React's behaviour when updating state with identical JS value,
  but using Clojure's value equality here"
  [updater]
  (react/useCallback
   (fn [v & args]
     (updater
      (fn [cv]
        (if (fn? v)
          (choose-value (apply v cv args) cv)
          (choose-value v cv)))))
   #js [updater]))

;; == State hook ==

(defn use-state [value]
  (let [[state set-state] (r/useState value)
        set-state (use-clojure-aware-updater set-state)]
    #js [state set-state]))

(defn- clojure-aware-reducer-updater
  "Same as `use-clojure-primitive-aware-updater` but for `use-reducer`"
  [f]
  (fn [state action]
    (choose-value (f state action) state)))

(defn use-reducer
  ([f value]
   (let [updater (clojure-aware-reducer-updater f)]
     (r/useReducer updater value)))
  ([f value init-state]
   (let [updater (clojure-aware-reducer-updater f)]
     (r/useReducer updater value init-state))))

;; == Ref hook

(defn use-ref [value]
  (r/useRef value))

;; == Effect hook ==
(defn with-return-value-check [f]
  #(let [ret (f)]
     (if (fn? ret) ret js/undefined)))

(defn use-clj-deps [deps]
  (let [ref (r/useRef deps)]
    (when (not= (.-current ref) deps)
      (set! (.-current ref) deps))
    (.-current ref)))

(defn use-effect
  ([setup-fn]
   (r/useEffect (with-return-value-check setup-fn)))
  ([setup-fn deps]
   (r/useEffect
    (with-return-value-check setup-fn)
    deps)))

;; == Layout effect hook ==
(defn use-layout-effect
  ([setup-fn]
   (r/useLayoutEffect
    (with-return-value-check setup-fn)))
  ([setup-fn deps]
   (r/useLayoutEffect
    (with-return-value-check setup-fn)
    deps)))

;; == Insertion effect hook ==
(defn use-insertion-effect
  ([f]
   (r/useInsertionEffect
    (with-return-value-check f)))
  ([f deps]
   (r/useInsertionEffect
    (with-return-value-check f)
    deps)))

;; == Callback hook ==
(defn use-callback
  [f deps]
  (r/useCallback f deps))

;; == Memo hook ==
(defn use-memo
  [f deps]
  (r/useMemo f deps))

;; == Context hook ==
(defn use-context [v]
  (r/useContext v))

;; == Imperative Handle hook ==
(defn use-imperative-handle
  ([ref create-handle]
   (r/useImperativeHandle ref create-handle))
  ([ref create-handle deps]
   (r/useImperativeHandle ref create-handle deps)))

;; == Debug hook ==
(defn use-debug
  ([v]
   (use-debug v nil))
  ([v fmt]
   (r/useDebugValue v fmt)))

(defn use-deferred-value [v]
  (r/useDeferredValue v))

(defn use-transition []
  (r/useTransition))

(defn use-id []
  (r/useId))

(defn use-sync-external-store
  ([subscribe get-snapshot]
   (r/useSyncExternalStore subscribe get-snapshot))
  ([subscribe get-snapshot get-server-snapshot]
   (r/useSyncExternalStore subscribe get-snapshot get-server-snapshot)))
