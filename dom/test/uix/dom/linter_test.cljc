(ns uix.dom.linter-test
  #?(:cljs (:require [uix.core :as uix :refer [$]]
                     [uix.dom]))
  #?(:clj (:require [clojure.string :as str]
                    [clojure.test :refer :all]
                    [shadow.cljs.devtools.cli])))

#?(:clj
   (deftest test-dom-linter
     (let [out-str (with-out-str (shadow.cljs.devtools.cli/-main "compile" "linter-test"))]
       (testing "should fail on invalid DOM property"
         (is (str/includes? out-str "Invalid DOM property :autoplay. Did you mean :auto-play?")))
       (testing "should not fail on invalid DOM property"
         (is (not (str/includes? out-str "Invalid DOM property :auto-play"))))
       (testing "should not fail on aliased DOM property"
         (is (not (str/includes? out-str "Invalid DOM property :class")))
         (is (not (str/includes? out-str "Invalid DOM property :for")))
         (is (not (str/includes? out-str "Invalid DOM property :charset")))
         (is (not (str/includes? out-str "Invalid DOM property :class-id")))
         (is (not (str/includes? out-str "Invalid DOM property :item-id")))))))

#?(:cljs
   (do
     ($ :div {:autoplay true})
     ($ :div {:auto-play true})

     ($ :div {:class true})
     ($ :div {:for true})
     ($ :div {:charset true})
     ($ :div {:class-id true})
     ($ :div {:item-id true})))