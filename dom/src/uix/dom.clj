(ns uix.dom
  (:require [uix.linter]
            [uix.dom.linter]
            [uix.lib]))

(defmethod uix.linter/lint-element :dom/invalid-attribute [type form env]
  (let [[tag attrs] (uix.lib/normalize-element env (rest form))]
    (when (and (keyword? tag) (map? attrs))
      (uix.dom.linter/validate-attrs tag attrs {:env env}))))