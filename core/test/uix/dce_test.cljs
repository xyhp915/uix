(ns uix.dce-test
  (:require [uix.core :as uix :refer [defui $]]))

(defui a []
  ($ :div "WORLD"))

(defui b []
  ($ :div "HELLO" ($ a)))
