(ns uix.ssr
  "Examples of integrating JS components into SSR on JVM"
  (:require #?(:clj [clojure.java.shell :as sh])
            [uix.core :as uix :refer [$ defui]]
            #?(:cljs ["react-slider" :as react.slider])
            #?(:clj [uix.dom.server :as dom.server])))

;; ========== Render JS components on the client ONLY ==========

#?(:cljs
    ;; renders JS component on the client after
    ;; hydration, so that React doesn't complain
    ;; about DOM mismatch.
    ;; This effectively makes JS components client-only
    (defui js-comp* [{:keys [children]}]
      (let [[show? set-show] (uix/use-state false)]
        (uix/use-effect
          #(set-show true)
          [])
        (when show? children))))

#?(:clj
   ;; Renders JS components only in CLJS
   (defmacro js-comp [comp & args]
     (when (uix.lib/cljs-env? &env)
       `($ js-comp* ($ ~comp ~@args)))))

(defui app []
  ($ :<>
    ($ :h1 "Hello")
    ;; the JS component is rendered only in CLJS
    (js-comp react.slider/ReactSlider)))

#?(:clj
   (dom.server/render-to-string ($ app)))

;; ========== Render placeholder components on JVM ==========

#?(:clj
    (defmulti with-placeholder* identity))

#?(:clj
    (defmethod with-placeholder* 'react.slider/ReactSlider [_]
      ($ :div
         "{{DOM structure that matches the JS component structure}}")))

#?(:clj
    (defmacro with-placeholder [comp & args]
      (if (uix.lib/cljs-env? &env)
        `($ ~comp ~@args)
        (with-placeholder* comp))))

(defui app []
  ($ :<>
     ($ :h1 "Hello")
     ;; renders JS component on the client
     ;; and HTML that matches the JS component structure on JVM
     (with-placeholder react.slider/ReactSlider)))

#?(:clj
   (dom.server/render-to-string ($ app)))

;; ========== Render JS components in Node sub process ==========

#?(:clj
   (defmacro with-js-ssr [shel-cmd comp & args]
     (if (uix.lib/cljs-env? &env)
       `($ ~comp ~@args)
       (:out (apply sh/sh shel-cmd)))))

(defui app []
  ($ :<>
     ($ :h1 "Hello")
     ;; renders JS component on the client
     ;; and the same component in Node sub process on JVM
     (with-js-ssr
       "{{shell command that runs Node.js script to render JS component in Node.js
          and spits rendered HTML to stdout}}"
       react.slider/ReactSlider)))

#?(:clj
   (dom.server/render-to-string ($ app)))
