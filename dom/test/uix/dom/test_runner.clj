(ns uix.dom.test-runner
  (:require [clojure.test :refer :all]
            [uix.dom.server-test]))

(defn -main [& args]
  (let [{:keys [fail]} (run-tests
                        'uix.dom.server-test)]
    (if (pos? fail)
      (System/exit 1)
      (System/exit 0))))
