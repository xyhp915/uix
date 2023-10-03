(ns uix.benchmark
  (:require-macros [uix.benchmark :refer [bench]])
  (:require [reagent.core :as r]
            ["react-dom/server" :as rserver]
            [react :as react]
            [uix.core :refer [defui $]]
            [uix.hiccup :as hiccup]
            [uix.react :refer [Editor]]))

(set! (.-React js/global) react)

(defn render [el]
      (rserver/renderToString el))

(def reagent-compiler
  (r/create-compiler {:function-components true}))

(defn -main [& args]
  (js/console.log "Warming up...")
  (bench :react 10000 (render (react/createElement Editor)))
  (bench :uix-compiled 10000 (render ($ hiccup/editor-compiled)))
  (bench :reagent-interpret 10000 (render (r/as-element [hiccup/editor])))

  (js/console.log "Running the benchmark...")
  (bench :react 10000 (render (react/createElement Editor)))
  (bench :uix-compiled 10000 (render ($ hiccup/editor-compiled)))
  (bench :reagent-interpret 10000 (render (r/as-element [hiccup/editor]))))
