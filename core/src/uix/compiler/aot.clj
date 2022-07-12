(ns uix.compiler.aot
  "Compiler code that translates HyperScript into React calls at compile-time."
  (:require [uix.compiler.js :as js]
            [uix.compiler.attributes :as attrs]
            [cljs.analyzer :as ana]
            [uix.lib]
            [uix.hooks.linter :as hooks.linter]))

(defmulti compile-attrs
  "Compiles a map of attributes into JS object,
  or emits interpretation call for runtime, when a value
  at props position is dynamic (symbol)"
  (fn [tag attrs opts] tag))

(defmethod compile-attrs :element [_ attrs {:keys [tag-id-class]}]
  (if (or (map? attrs) (nil? attrs))
    `(cljs.core/array
      ~(cond-> attrs
         ;; interpret :style if it's not map literal
         (and (some? (:style attrs))
              (not (map? (:style attrs))))
         (assoc :style `(uix.compiler.attributes/convert-props ~(:style attrs) (cljs.core/array) true))
         ;; merge parsed id and class with attrs map
         :always (attrs/set-id-class tag-id-class)
         ;; camel-casify the map
         :always (attrs/compile-attrs {:custom-element? (last tag-id-class)})
         ;; emit JS object literal
         :always js/to-js))
    ;; otherwise emit interpretation call
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array ~@tag-id-class) false)))

(defmethod compile-attrs :component [_ props _]
  (if (or (map? props) (nil? props))
    `(cljs.core/array ~props)
    `(uix.compiler.attributes/interpret-props ~props)))

(defmethod compile-attrs :fragment [_ attrs _]
  (if (map? attrs)
    `(cljs.core/array ~(-> attrs attrs/compile-attrs js/to-js))
    `(uix.compiler.attributes/interpret-attrs ~attrs (cljs.core/array) false)))

(defn- input-component? [x]
  (contains? #{"input" "textarea"} x))

(defn- uix-element?
  "Returns true when `form` is `(uix.core/$ ...)`"
  [env form]
  (and (list? form)
       (symbol? (first form))
       (->> (first form) (ana/resolve-var env) :name (= 'uix.core/$))))

(def elements-list-fns
  '#{for map mapv filter filterv remove keep keep-indexed})

(defn- elements-list?
  "Returns true when `v` is form commonly used to render a list of elements
  `(map ...)`, `(for ...)`, etc"
  [v]
  (and (list? v)
       (symbol? (first v))
       (elements-list-fns (first v))))

(defn- normalize-element
  "When the second item in the element `v` is either UIx element or `elements-list?`,
  returns normalized element with empty map at props position
  and child element shifted into children position"
  [env v]
  (if (or (uix-element? env (second v))
          (elements-list? (second v)))
    (into [(first v) {}] (rest v))
    v))

(defn- uix-component? [tag env]
  (and (symbol? tag)
       (uix.lib/uix-component-var? (ana/resolve-var env tag))))

(defn- interop-element? [tag]
  (or (symbol? tag)
      (and (list? tag)
           (not= 'quote (first tag)))))

(defmulti compile-element*
  "Compiles UIx elements into React.createElement"
  (fn [[tag] {:keys [env]}]
    (cond
      (= :<> tag) :fragment
      (keyword? tag) :element
      (uix-component? tag env) :component
      (interop-element? tag) :interop)))

(defmethod compile-element* :default [[tag] {:keys [env]}]
  (ana/warning ::unknown-element-type env {:tag tag}))

(defmethod ana/error-message ::unknown-element-type [_ {:keys [tag]}]
  (str
   (cond
     (string? tag) "`uix.core/$` was passed as string as element type, which is not supported.\n"
     (and (list? tag) (= 'quote (first tag))) "`uix.core/$` was passed quoted symbol as element type, which is not supported.\n"
     :else (str "Unknown element type `" tag "` of type " (type tag) " was passed into `$`.\n"))
   "UIx expects element type to be one of: :<> (fragment), :div (keyword) or symbol (defui or plain react component)"))

(defmethod compile-element* :element [v {:keys [env]}]
  (let [[tag attrs & children] (normalize-element env v)
        tag-id-class (attrs/parse-tag tag)
        attrs-children (compile-attrs :element attrs {:tag-id-class tag-id-class})
        tag-str (first tag-id-class)
        ret (if (input-component? tag-str)
              `(create-uix-input ~tag-str ~attrs-children (cljs.core/array ~@children))
              `(>el ~tag-str ~attrs-children (cljs.core/array ~@children)))]
    ret))

(defmethod compile-element* :component [v {:keys [env]}]
  (let [[tag props & children] (normalize-element env v)
        tag (vary-meta tag assoc :tag 'js)
        props-children (compile-attrs :component props nil)]
    `(uix.compiler.alpha/uix-component-element ~tag ~props-children (cljs.core/array ~@children))))

(defmethod compile-element* :interop [v {:keys [env]}]
  (let [[tag props & children] (normalize-element env v)
        tag (vary-meta tag assoc :tag 'js)
        props-children (compile-attrs :component props nil)]
    `(uix.compiler.alpha/react-component-element ~tag ~props-children (cljs.core/array ~@children))))

(defmethod compile-element* :fragment [v _]
  (let [[_ attrs & children] v
        attrs (compile-attrs :fragment attrs nil)
        ret `(>el fragment ~attrs (cljs.core/array ~@children))]
    ret))

(defn compile-element [v {:keys [env] :as opts}]
  (if (uix.lib/cljs-env? env)
    (do
      (hooks.linter/lint-element v opts)
      (compile-element* v opts))
    v))
