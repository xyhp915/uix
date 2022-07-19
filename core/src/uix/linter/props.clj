(ns uix.linter.props
  (:require [cljs.analyzer :as ana]
            [clojure.string :as str]
            [cljs.spec.alpha]))

(defmethod ana/error-message ::props-map [_ {:keys [component required-keys]}]
  (str "\nInvalid props value passed into UIx component `" component "`\n"
       "Expects a map literal with the following set of keys: " required-keys))

(defmethod ana/error-message ::missing-props [_ {:keys [missing-keys component required-keys provided-keys children]}]
  (let [only-children-missing? (= #{:children} missing-keys)
        only-children-required? (= #{:children} required-keys)
        only-children-provided? (= #{:children} provided-keys)]
    (str "\nMissing "
         (if only-children-missing?
           "child elements"
           (str "props " missing-keys))
         " in UIx component `" component "`\n"
         (when-not (and only-children-missing? only-children-required?)
           "The component expects the following set of keys: " required-keys "\n")
         (when-not only-children-missing?
           (cond
             only-children-provided?
             (str "Instead got only the following child elements: `" (str/join " " children) "`\n")

             (seq provided-keys)
             (str "Instead got the following set of keys: " provided-keys "\n"))))))

(defmethod ana/error-message ::unexpected-props [_ {:keys [unexpected-keys component required-keys optional-keys children]}]
  (let [only-children-required? (and (empty? optional-keys) (= #{:children} required-keys))
        only-children-unexpected? (= #{:children} unexpected-keys)]
    (str (if only-children-unexpected?
           (str "\nUnexpected child elements")
           (str "\nUnexpected props " unexpected-keys))
         (str " passed to UIx component `" component "`\n")
         (when (unexpected-keys :children)
           (str "The component doesn't expect any child elements, but got `" (str/join " " children) "` instead\n"))
         (if only-children-required?
           "The component expects only child elements\n"
           (str "The component expects the following set of keys: " required-keys "\n"
                (cond
                  (= #{:children} optional-keys) (str "and optional child elements\n")
                  (seq optional-keys) (str "and the following set of optional keys: " optional-keys "\n")))))))

(def ^:private props-specs-registry (atom {}))

(defn register-props-spec!
  "Associates component name with provided :props spec
  The spec is used to check provided props at component usage place ($ ...)"
  [env component-sym props-spec]
  (let [sym (uix.lib/ns-qualify env component-sym)]
    (swap! props-specs-registry assoc sym props-spec)))

(defn- get-spec-keys [un-key q-key spec]
  (->> (get spec un-key)
       (map (comp keyword name))
       (concat (get spec q-key))
       set))

(defn- parse-keys-spec-form [spec-form]
  (->> (rest spec-form)
       (partition 2)
       (reduce (fn [ret [k v]]
                 (assoc ret k v))
               {})))

(defn- props-env [props env]
  (if (some? props)
    (select-keys (meta props) [:line :column])
    env))

(defn assert-props-spec*
  "Asserts provided `props` and `children` against `spec-form` registered for a given `component-name`"
  [env spec-form component-name props children]
  (when (= 'cljs.spec.alpha/keys (first spec-form))
    (let [spec-map (parse-keys-spec-form spec-form)
          req-ks (get-spec-keys :req-un :req spec-map)
          opt-ks (get-spec-keys :opt-un :opt spec-map)
          spec-ks (into req-ks opt-ks)]
      (if (and (not (map? props)) (not= #{:children} req-ks))
        ;; expected some props, but got a non map literal value instead
        [[::props-map (props-env props env)
          {:component component-name
           :required-keys req-ks
           :optional-keys opt-ks}]]
        (let [props (cond
                      ;; when a child element is passed instead of props map
                      (and (not (map? props)) (some? props))
                      {:children (into [props] children)}

                      ;; when child elements are passed as rest args
                      (and (map? props) (seq children))
                      (assoc props :children children)

                      :else props)
              children (cond
                         (nil? (:children props)) []
                         (not (coll? (:children props))) [(:children props)]
                         :else (:children props))
              props-keys (set (keys props))
              missing-keys (set (filter (comp not props-keys) req-ks))
              unexpected-keys (set (filter (comp not spec-ks) props-keys))]
          (cond-> []
                  (seq missing-keys)
                  ;; some keys are missing from props map
                  (conj [::missing-props (props-env props env)
                         {:component component-name
                          :missing-keys missing-keys
                          :required-keys req-ks
                          :provided-keys props-keys
                          :children children}])

                  (seq unexpected-keys)
                  ;; some keys are not expected to be in props map
                  (conj [::unexpected-props (props-env props env)
                         {:component component-name
                          :unexpected-keys unexpected-keys
                          :required-keys req-ks
                          :optional-keys opt-ks
                          :children children}])))))))

(defn assert-props-spec
  "Performs a spec check for a component in ($ ...),
  if the component has :props spec registered"
  [env tag props children]
  (when (symbol? tag)
    (let [sym (uix.lib/ns-qualify env tag)]
      (when-let [spec-form (@cljs.spec.alpha/registry-ref (@props-specs-registry sym))]
        (doseq [error (assert-props-spec* env spec-form sym props children)]
          (apply ana/warning error))))))

(defn parse-conds
  "Parses :props/open & :props/closed conditions, :pre and :post conditions are preserved"
  [fdecl]
  (let [conds (when (and (next fdecl) (map? (first fdecl)))
                (first fdecl))
        props-cond (when conds
                     (uix.lib/assert! (not (and (:props/open conds) (:props/closed conds)))
                                      "Can’t have both :props/open and :props/closed specs")
                     (cond
                       (:props/open conds) [:props/open (:props/open conds)]
                       (:props/closed conds) [:props/closed (:props/closed conds)]))
        body (if conds (next fdecl) fdecl)
        body (if props-cond
               (concat [(dissoc conds :props/open :props/closed)] body)
               body)]
    [body props-cond]))

(defn make-props-check
  "Parses :props/open & :props/closed conditions from component's body and registers provided spec
  Returns body of the component and spec name"
  [env sym fdecl]
  (let [[fdecl [spec-type props-spec]] (parse-conds fdecl)]
    (if-not props-spec
      [fdecl nil]
      (do
        (when (= :props/closed spec-type)
          (register-props-spec! env sym props-spec))
        [fdecl props-spec]))))

