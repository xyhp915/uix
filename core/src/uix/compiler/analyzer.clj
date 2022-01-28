(ns uix.compiler.analyzer
  (:require [clojure.walk]))

(defn hook-sym? [x]
  (boolean
    (and (symbol? x)
         (some #(re-find % (name x))
               [#"^use\-"
                #"^use[A-Z]"]))))

(defn hook-expr? [x]
  (and (list? x) (hook-sym? (first x))))

(defn find-hooks [body]
  (let [hooks (atom [])]
    (clojure.walk/prewalk
      (fn [x]
        (when (hook-expr? x)
          (swap! hooks conj x))
        x)
      body)
    @hooks))
