(ns uix.dev
  (:require ["react-refresh/runtime" :as refresh]))

(defn init! []
  (refresh/injectIntoGlobalHook js/window))

(set! (.-$$Register$$ js/window) refresh/register)
(set! (.-$$Signature$$ js/window) refresh/createSignatureFunctionForTransform)

(defn refresh! []
  (refresh/performReactRefresh))

(defn signature! []
  (when (exists? (.-$$Signature$$ js/window))
    (.$$Signature$$ js/window)))

(defn register! [type id]
  (when (exists? (.-$$Register$$ js/window))
    (.$$Register$$ js/window type id)))
