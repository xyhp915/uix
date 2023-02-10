(ns uix.assertions
  (:require [clojure.test :as t]))

(defmethod t/assert-expr 'not-thrown? [msg form]
  ;; (is (thrown? c expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Returns the exception thrown.
  (let [body (nthnext form 1)]
    `(try ~@body
          (t/do-report {:type :pass, :message ~msg,
                        :expected nil, :actual nil})
          (catch Throwable e#
            (t/do-report {:type :fail, :message ~msg,
                          :expected "No exception", :actual e#})
            e#))))
