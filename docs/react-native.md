# Using React Native with UIx

1. Create RN project
```sh
npx react-native init MyApp
cd MyApp
echo 'import "./app/index.js";' > index.js
```

2. Add Clojure deps
```clojure
;; deps.edn
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        org.clojure/clojurescript {:mvn/version "1.11.60"}
        com.pitch/uix.core {:mvn/version "0.10.0"}
        thheller/shadow-cljs {:mvn/version "2.19.8"}}
 :paths ["src" "dev"]}
```

3. Add build config
```clojure
;; shadow-cljs.edn
{:deps true
 :builds {:app
          {:target :react-native
           :init-fn app.core/init
           :output-dir "app"
           :compiler-options {:source-map-path "app/"
                              :source-map-include-sources-content true
                              :warnings-as-errors true}
           :devtools {:preloads [app.preload]
                      :build-notify app.preload/build-notify}}}}
```

4. Setup dev tooling
```clojure
;; dev/app/preload.cljs
(ns app.preload
    (:require [uix.dev]
              [clojure.string :as str]))

;; Initializes fast-refresh runtime.
(defonce __init-fast-refresh!
  (do (uix.dev/init-fast-refresh!)
      nil))

;; Called by shadow-cljs after every reload.
(defn ^:dev/after-load refresh []
  (uix.dev/refresh!))

;; Forwards cljs build errors to React Native's error view
(defn build-notify [{:keys [type report]}]
  (when (= :build-failure type)
    (let [lines (str/split-lines report)
          err (js/Error. (nth lines 8))]
      (js/console.error err))))

```

5. Add some UI code
```clojure
;; src/app/core.cljs
(ns app.core
  (:require [react-native :as rn]
            [uix.core :refer [$ defui]]))

(defui root []
  ($ :view {:style {:flex 1
                    :align-items :center
                    :justify-content :center}} 
    ($ :text {:style {:font-size 32
                      :font-weight "500"
                      :text-align :center}}
      "Hello! 👋")))

(defn start []
  (.registerComponent rn/AppRegistry "MyApp" (constantly root)))
```
