(ns uix.dom.test-runner
  (:require [clojure.test :refer :all]
            [uix.dom.server-test]))

(defn -main [& args]
  (let [{:keys [fail error]} (run-tests
                              'uix.dom.server-test)]
    (if (pos? (+ fail error))
      (System/exit 1)
      (System/exit 0))))
