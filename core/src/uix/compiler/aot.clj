(ns uix.compiler.aot
  "Compiler code that translates HyperScript into React calls at compile-time."
  (:require [uix.compiler.js :as js]
            [uix.compiler.attributes :as attrs]
            [clojure.set :as set]
            [cljs.analyzer :as ana]
            [uix.lib]
            [clojure.string :as str]))

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

(defmulti compile-element*
  "Compiles UIx elements into React.createElement"
  (fn [[tag] _]
    (cond
      (= :<> tag) :fragment
      (keyword? tag) :element
      :else :component)))

(defmethod compile-element* :element [v {:keys [env]}]
  (let [[tag attrs & children] (normalize-element env v)
        tag-id-class (attrs/parse-tag tag)
        attrs-children (compile-attrs :element attrs {:tag-id-class tag-id-class})
        tag-str (first tag-id-class)
        ret (if (input-component? tag-str)
              `(create-uix-input ~tag-str ~attrs-children (cljs.core/array ~@children))
              `(>el ~tag-str ~attrs-children (cljs.core/array ~@children)))]
    ret))

(defn- validate-destructured-props [props-sig props component-name]
  (when (and (map? props) (map? props-sig) (= [:keys] (keys props-sig)))
    (let [actual-keys (disj (set (keys props)) :key)
          expected-keys (into #{} (map (comp keyword name)) (:keys props-sig))
          unexpected-props (set/difference actual-keys expected-keys)]
      (when (seq unexpected-props)
        (let [env (select-keys (meta props) [:line :column])]
          (ana/warning ::unexpected-props env {:unexpected-props (vec unexpected-props)
                                               :expected-props (vec expected-keys)
                                               :component-name component-name}))))))

(defn- validate-no-props-expected [args [tag & props] component-name]
  (when (and (or (empty? args) (= '_ (first args)))
             (seq props))
    (let [env (select-keys (or (meta (first props)) (meta tag))
                           [:line :column])]
      (ana/warning ::no-props-expected env {:component-name component-name}))))

(defmethod ana/error-message ::unexpected-props [_ {:keys [unexpected-props expected-props component-name]}]
  (str "Invalid props: UIx component `" component-name "` expects only the following props: " (str/join ", " expected-props) ",\n"
       "but also got additional set of props: " (str/join ", " unexpected-props) ".\n"
       "Is that a typo? Either fix it or remove unused props since they can cause unnecessary updates of the component."))

(defmethod ana/error-message ::no-props-expected [_ {:keys [component-name]}]
  (str "Invalid props: UIx component `" component-name "` doesn't expect any props.\n"
       "Should the component take props? Either fix it or remove the props since they can cause unnecessary updates of the component."))

(defn- validate-signature [[tag props & children :as el] env]
  (when (symbol? tag)
    (let [v (ana/resolve-var env tag)]
      (when-let [args (-> v :meta :uix/args second)]
        (validate-no-props-expected args el (:name v))
        (validate-destructured-props (first args) props (:name v))))))

(defmethod compile-element* :component [v {:keys [env]}]
  (let [[tag props & children :as el] (normalize-element env v)
        _ (validate-signature el env)
        tag (vary-meta tag assoc :tag 'js)
        props-children (compile-attrs :component props nil)]
    `(uix.compiler.alpha/component-element ~tag ~props-children (cljs.core/array ~@children))))

(defmethod compile-element* :fragment [v _]
  (let [[_ attrs & children] v
        attrs (compile-attrs :fragment attrs nil)
        ret `(>el fragment ~attrs (cljs.core/array ~@children))]
    ret))

(defn compile-element [v {:keys [env] :as opts}]
  (if (uix.lib/cljs-env? env)
    (compile-element* v opts)
    v))
