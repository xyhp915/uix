(ns uix.hooks.alpha
  "Wrappers for React Hooks"
  (:require [react :as r]))

;; == State hook ==

(defn use-state [value]
  (r/useState value))

(defn use-reducer
  ([f value]
   (r/useReducer #(f %1 %2) value))
  ([f value init-state]
   (r/useReducer #(f %1 %2) value init-state)))

;; == Ref hook

(defn use-ref [value]
  (r/useRef value))

(defn with-return-value-check [f]
  #(let [ret (f)]
     (if (fn? ret) ret js/undefined)))

;; == Effect hook ==
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
  ([f]
   (r/useCallback f))
  ([f deps]
   (r/useCallback f deps)))

;; == Memo hook ==
(defn use-memo
  ([f]
   (r/useMemo f))
  ([f deps]
   (r/useMemo f deps)))

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
