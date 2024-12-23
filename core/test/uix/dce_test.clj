(ns uix.dce-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [shadow.cljs.devtools.cli]))

(deftest test-dce
  (testing "should not include unused component"
    (with-out-str (shadow.cljs.devtools.cli/-main "release" "dce-test"))
    (is (not (str/includes? (slurp "out/main.js") "uix.dce-test/a")))
    (is (not (str/includes? (slurp "out/main.js") "uix.dce-test/b")))))
