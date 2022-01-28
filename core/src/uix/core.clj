(ns uix.core
  "Public API"
  (:require [uix.compiler.aot]
            [uix.source]
            [cljs.core]
            [clojure.string :as str]
            [uix.compiler.analyzer :as ana]))

(defn- no-args-component [sym body]
  `(defn ~sym []
     ~@body))

(defn- with-args-component [sym args body]
  `(defn ~sym [props#]
     (let [~args (cljs.core/array (glue-args props#))
           f# (fn [] ~@body)]
       (f#))))

(defn parse-sig [name fdecl]
  (let [[fdecl m] (if (string? (first fdecl))
                    [(next fdecl) {:doc (first fdecl)}]
                    [fdecl {}])
        [fdecl m] (if (map? (first fdecl))
                    [(next fdecl) (conj m (first fdecl))]
                    [fdecl m])
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        [fdecl m] (if (map? (last fdecl))
                    [(butlast fdecl) (conj m (last fdecl))]
                    [fdecl m])
        m (conj {:arglists (list 'quote (#'cljs.core/sigs fdecl))} m)
        m (conj (if (meta name) (meta name) {}) m)]
    (assert (= 1 (count fdecl)) (str `defui " doesn't support multi-arity"))
    (let [[args & fdecl] (first fdecl)]
      (assert (>= 1 (count args))
              (str `defui " should be a single-arity component taking a map of props, found: " args "\n"
                   "If you meant to retrieve `children`, they are under `:children` field in props map"))
      [(with-meta name m) args fdecl])))

(defmacro
  ^{:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])}
  defui
  "Compiles UIx component into React component at compile-time."
  [sym & fdecl]
  (let [[fname args fdecl] (parse-sig sym fdecl)
        ns-name (-> &env :ns :name str)
        sig-sym (gensym "sig")
        dev-check (with-meta 'goog.DEBUG {:tag 'boolean})
        body (cons `(when ~dev-check
                      (when ~sig-sym (~sig-sym)))
                   fdecl)]
    (uix.source/register-symbol! sym)
    `(do
       (when ~dev-check
         (def ~sig-sym (uix.dev/signature!)))
       ~(if (empty? args)
          (no-args-component fname body)
          (with-args-component fname args body))
       (when ~dev-check
         (when ~sig-sym
           (~sig-sym ~sym ~(str/join (ana/find-hooks body)) nil nil)
           (uix.dev/register! ~sym ~(str ns-name "/" sym))))
       (set! (.-uix-component? ~(with-meta sym {:tag 'js})) true)
       (with-name ~sym ~ns-name ~(str sym)))))

(defmacro source
  "Returns source string of UIx component"
  [sym]
  (uix.source/source sym))
