(ns uix.compiler.aot
  "Compiler code that translates HyperScript into React calls at compile-time."
  (:require [uix.compiler.js :as js]
            [uix.compiler.attributes :as attrs]))

(defmulti compile-attrs (fn [tag attrs opts] tag))

(defmethod compile-attrs :element [_ attrs {:keys [tag-id-class]}]
  (if (or (map? attrs) (nil? attrs))
    `(cljs.core/array
       ~(cond-> attrs
                (and (some? (:style attrs))
                     (not (map? (:style attrs))))
                (assoc :style `(uix.compiler.attributes/convert-props ~(:style attrs) (cljs.core/array) true))
                :always (attrs/set-id-class tag-id-class)
                :always (attrs/compile-attrs {:custom-element? (last tag-id-class)})
                :always js/to-js))
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array ~@tag-id-class) false)))

(defmethod compile-attrs :component [_ props _]
  `(uix.compiler.attributes/interpret-props ~props))

(defmethod compile-attrs :fragment [_ attrs _]
  (if (map? attrs)
    `(cljs.core/array ~(-> attrs attrs/compile-attrs js/to-js))
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array) false)))

(defmethod compile-attrs :suspense [_ attrs _]
  (if (map? attrs)
    `(cljs.core/array ~(-> attrs attrs/compile-attrs js/to-js))
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array) false)))

(defmethod compile-attrs :interop [_ props _]
  (if (map? props)
    `(cljs.core/array
       ~(cond-> props
                :always (attrs/compile-attrs {:custom-element? true})
                :always (js/to-js-map true)))
    `(uix.compiler.attributes/interpret-attrs ~props (cljs.core/array) true)))

;; Compiles HyperScript into React.createElement
(defmulti compile-element
  (fn [[tag]]
    (cond
      (= :<> tag) :fragment
      (= :# tag) :suspense
      (= :> tag) :interop
      (keyword? tag) :element
      :else :component)))

(defn- input-component? [x]
  (contains? #{"input" "textarea"} x))

(defmethod compile-element :element [v]
  (let [[tags attrs & children] v]
    (loop [tree (reverse (attrs/parse-tags tags))
           children children]
      (let [tag-id-class (first tree)
            attrs-children (compile-attrs :element attrs {:tag-id-class tag-id-class})
            tag-str (first tag-id-class)
            el (if (input-component? tag-str)
                 `(create-uix-input ~tag-str ~attrs-children (cljs.core/array ~@children))
                 `(>el ~tag-str ~attrs-children (cljs.core/array ~@children)))]
        (if (empty? (rest tree))
          el
          (recur (rest tree) (list el)))))))

(defmethod compile-element :component [v]
  (let [[tag props & children] v
        tag (vary-meta tag assoc :tag 'js)
        props-children (compile-attrs :component props nil)]
    `(uix.compiler.alpha/component-element ~tag ~props-children (cljs.core/array ~@children))))

(defmethod compile-element :fragment [v]
  (let [[_ attrs & children] v
        attrs (compile-attrs :fragment attrs nil)
        ret `(>el fragment ~attrs (cljs.core/array ~@children))]
    ret))

(defmethod compile-element :suspense [v]
  (let [[_ attrs & children] v
        attrs (compile-attrs :suspense attrs nil)
        ret `(>el suspense ~attrs (cljs.core/array ~@children))]
    ret))

(defmethod compile-element :interop [v]
  (let [[_ tag props & children] v
        props (compile-attrs :interop props nil)]
    `(>el ~tag ~props (cljs.core/array ~@children))))
