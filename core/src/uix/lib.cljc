(ns uix.lib
  #?(:cljs (:require-macros [uix.lib :refer [doseq-loop]]))
  #?(:cljs (:require [goog.object :as gobj]))
  #?(:clj (:require [clojure.walk]
                    [cljs.analyzer :as ana])))

#?(:clj
   (defmacro assert! [x message]
     `(when-not ~x
        (throw (new AssertionError (str "Assert failed: " ~message "\n" (pr-str '~x)))))))

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

#?(:clj
   (defn cljs-env? [env]
     (boolean (:ns env))))

#?(:clj
   (defn find-form [pred sexp]
     (let [forms (atom [])]
       (clojure.walk/prewalk
        (fn [x]
          (when (pred x)
            (swap! forms conj x))
          x)
        sexp)
       @forms)))

#?(:clj
   (defn- ->sym
     "Returns a symbol from a symbol or var"
     [x]
     (if (map? x)
       (:name x)
       x)))

#?(:clj
   (defn ns-qualify
     "Qualify symbol s by resolving it or using the current *ns*."
     [env s]
     (if (namespace s)
       (binding [ana/*private-var-access-nowarn* true]
         (->sym (ana/resolve-var env s)))
       (symbol (str ana/*cljs-ns*) (str s)))))
