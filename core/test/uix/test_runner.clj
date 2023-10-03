(ns uix.test-runner
  (:require [clojure.test :refer :all]
            [uix.core-test]
            [uix.aot-test]
            [uix.linter-test]
            [uix.hooks-test]))

(defn -main [& args]
  (let [{:keys [fail]} (run-tests
                        'uix.core-test
                        'uix.aot-test
                        'uix.hooks-test
                        'uix.linter-test)]
    (when (pos? fail)
      (System/exit 1))))
