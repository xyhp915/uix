(ns uix.hooks.alpha
  "Wrappers for React Hooks"
  (:require [goog.object :as gobj]
            [react :as r]))

;; == State hook ==

(defn use-state [value]
  (r/useState value))

;; == Ref hook

(defn use-ref [value]
  (let [ref (r/useRef nil)]
    (when (nil? (.-current ref))
      (set! (.-current ref)
            (specify! #js {:current value}
              IDeref
              (-deref [this]
                (.-current this))

              IReset
              (-reset! [this v]
                (gobj/set this "current" v)))))
    (.-current ref)))

(defn- with-deps-check
  ([hook-fn f deps]
   (with-deps-check hook-fn nil f deps))
  ([hook-fn ref f deps]
   (let [prev-deps (r/useRef deps)
         id (r/useRef 0)
         cb #(let [ret (f)]
               (set! (.-current prev-deps) deps)
               (if (fn? ret) ret js/undefined))
         _ (when (not= (.-current prev-deps) deps)
             (set! (.-current id) (inc (.-current id))))
         deps #js [(.-current id)]]
     (if ^boolean ref
       (hook-fn ref cb deps)
       (hook-fn cb deps)))))

;; == Effect hook ==
(defn use-effect
  ([setup-fn]
   (r/useEffect
     #(let [ret (setup-fn)]
        (if (fn? ret) ret js/undefined))))
  ([setup-fn deps]
   (with-deps-check r/useEffect setup-fn deps)))

;; == Layout effect hook ==
(defn use-layout-effect
  ([setup-fn]
   (r/useLayoutEffect
     #(let [ret (setup-fn)]
        (if (fn? ret) ret js/undefined))))
  ([setup-fn deps]
   (with-deps-check r/useLayoutEffect setup-fn deps)))

;; == Callback hook ==
(defn use-callback
  ([f]
   (r/useCallback f))
  ([f deps]
   (with-deps-check r/useCallback f deps)))

;; == Memo hook ==
(defn use-memo
  ([f]
   (r/useMemo f))
  ([f deps]
   (with-deps-check r/useMemo f deps)))

;; == Context hook ==
(defn use-context [v]
  (r/useContext v))

;; == Imperative Handle hook ==
(defn use-imperative-handle
  ([ref create-handle]
   (r/useImperativeHandle ref create-handle))
  ([ref create-handle deps]
   (with-deps-check r/useImperativeHandle ref create-handle deps)))

;; == Debug hook ==
(defn use-debug-value
  ([v]
   (use-debug-value v nil))
  ([v fmt]
   (r/useDebugValue v fmt)))
