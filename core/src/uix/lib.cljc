(ns uix.lib
  #?(:cljs (:require-macros [uix.lib :refer [doseq-loop]]))
  #?(:cljs (:require [goog.object :as gobj]
                     [cljs.analyzer :as ana]))
  #?(:clj (:require [cljs.analyzer :as ana]
                    [clojure.walk]
                    [cljs.core]))
  #?(:clj (:import (clojure.lang RT Util))))

(defn assert! [x message]
  (when-not x
    #?(:cljs (throw (js/Error. (str "Assert failed: " message "\n" (pr-str x))))
       :clj (throw (AssertionError. (str "Assert failed: " message "\n" (pr-str x)))))))

#?(:clj
   (defmacro doseq-loop [[v vs] & body]
     `(let [v# ~vs]
        (when (seq v#)
          (loop [x# (first v#)
                 xs# (next v#)]
            (let [~v x#]
              ~@body)
            (when (seq xs#)
              (recur (first xs#) (next xs#))))))))

#?(:cljs
   (defn map->js [m]
     (reduce-kv (fn [o k v]
                  (gobj/set o (name k) v)
                  o)
                #js {}
                m)))

(defn cljs-env? [env]
  (boolean (:ns env)))

(defn find-form [pred sexp]
  (let [forms (atom [])]
    (clojure.walk/prewalk
     (fn [x]
       (when (pred x)
         (swap! forms conj x))
       x)
     sexp)
    @forms))

(defn- sigs [fdecl]
  #_(assert-valid-fdecl fdecl)
  (let [asig
        (fn [fdecl]
          (let [arglist (first fdecl)
                ;elide implicit macro args
                arglist (if #?(:clj (Util/equals '&form (first arglist))
                               :cljs (= '&form (first arglist)))
                          #?(:clj (RT/subvec arglist 2 (RT/count arglist))
                             :cljs (subvec arglist 2 (count arglist)))
                          arglist)
                body (next fdecl)]
            (if (map? (first body))
              (if (next body)
                (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                arglist)
              arglist)))]
    (if (seq? (first fdecl))
      (loop [ret [] fdecls fdecl]
        (if fdecls
          (recur (conj ret (asig (first fdecls))) (next fdecls))
          (seq ret)))
      (list (asig fdecl)))))

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
        m (conj {:arglists (list 'quote (sigs fdecl))} m)
        m (conj (if (meta name) (meta name) {}) m)]
    [(with-meta name m) fdecl]))

(defn- uix-element?
  "Returns true when `form` is `(uix.core/$ ...)`"
  [env form]
  (let [resolve-fn #?(:clj (if (uix.lib/cljs-env? env)
                             ana/resolve-var
                             resolve)
                      :cljs ana/resolve-var)]
    (and (list? form)
         (symbol? (first form))
         (->> (first form) (resolve-fn env) :name (= 'uix.core/$)))))

(def elements-list-fns
  '#{for map mapv filter filterv remove keep keep-indexed})

(defn- elements-list?
  "Returns true when `v` is form commonly used to render a list of elements
  `(map ...)`, `(for ...)`, etc"
  [v]
  (and (list? v)
       (symbol? (first v))
       (elements-list-fns (first v))))

(defn normalize-element
  "When the second item in the element `v` is either UIx element or `elements-list?`,
  returns normalized element with empty map at props position
  and child element shifted into children position"
  [env v]
  (if (or (uix-element? env (second v))
          (elements-list? (second v)))
    (into [(first v) {}] (rest v))
    v))
