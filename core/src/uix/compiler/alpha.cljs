(ns uix.compiler.alpha
  "UTL and UIx components interpreter. Based on Reagent."
  (:require [react :as react]
            [uix.hooks.alpha :as hooks]
            [cljs-bean.core :as bean]
            [uix.compiler.debug :as debug]
            [uix.compiler.attributes :as attrs]))

(def ^:dynamic *default-compare-args* #(= (.-argv %1) (.-argv %2)))

(defn js-val? [x]
  (not (identical? "object" (goog/typeOf x))))

(defn symbol-for [s]
  (js* "Symbol.for(~{})" s))

(defn as-lazy-component [f]
  (debug/with-name f))

(defn as-react [f]
  #(f (bean/bean %)))

(defn uix-component-element [component-type ^js props-children children]
  (let [props (aget props-children 0)
        js-props (if-some [key (:key props)]
                   #js {:key key :argv (dissoc props :key)}
                   #js {:argv props})
        args (if (= 2 (.-length props-children))
               #js [component-type js-props (aget props-children 1)]
               #js [component-type js-props])]
    (.apply react/createElement nil (.concat args children))))

(defn react-component-element [component-type ^js props-children children]
  (let [raw-props (aget props-children 0)
        props (aget (attrs/interpret-attrs raw-props #js [] true) 0)]
    (.apply react/createElement nil (.concat #js [component-type props (aget props-children 1)] children))))

(defn component-element [^js component-type ^js props-children children]
  (if (.-uix-component? component-type)
    (uix-component-element component-type props-children children)
    (react-component-element component-type props-children children)))
