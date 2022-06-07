(ns uix.lib
  #?(:cljs (:require-macros [uix.lib :refer [doseq-loop]]))
  #?(:clj (:require [cljs.analyzer :as ana]
                    [cljs.core]))
  #?(:cljs (:require [goog.object :as gobj])))

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
   (defn re-seq*
     "Similar to cljs.core/re-seq, but eager and faster"
     [re s]
     (loop [s s
            matches (.exec re s)
            ret #js []]
       (let [match-str (aget matches 0)
             match-vals (if (== (.-length matches) 1)
                          match-str
                          matches)
             post-idx (+ (.-index matches) (max 1 (.-length match-str)))
             next-s (subs s post-idx)]
         (.push ret match-vals)
         (if (<= post-idx (.-length s))
           (if-some [next-matches (.exec re next-s)]
             (recur next-s next-matches ret)
             ret)
           ret)))))
#?(:clj
   (defn cljs-env? [env]
     (boolean (:ns env))))

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

#?(:cljs
   (defn map->js [m]
     (reduce-kv (fn [o k v]
                  (gobj/set o (name k) v)
                  o)
                #js {}
                m)))

#?(:clj
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
       [(with-meta name m) fdecl])))
