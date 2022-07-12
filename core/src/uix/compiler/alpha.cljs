(ns uix.compiler.alpha
  (:require [react]
            [uix.compiler.attributes :as attrs]))

(defn- uix-component-element [component-type ^js props-children children]
  (let [props (aget props-children 0)
        js-props (if-some [key (:key props)]
                   #js {:key key :argv (dissoc props :key)}
                   #js {:argv props})
        args (if (= 2 (.-length props-children))
               #js [component-type js-props (aget props-children 1)]
               #js [component-type js-props])]
    (.apply react/createElement nil (.concat args children))))

(defn- react-component-element [component-type ^js props-children children]
  (let [js-props (-> (aget props-children 0)
                     (attrs/interpret-attrs #js [] true)
                     (aget 0))
        args (if (= 2 (.-length props-children))
               #js [component-type js-props (aget props-children 1)]
               #js [component-type js-props])]
    (.apply react/createElement nil (.concat args children))))

(defn component-element [^clj component-type props-children children]
  (if (.-uix-component? component-type)
    (uix-component-element component-type props-children children)
    (react-component-element component-type props-children children)))
