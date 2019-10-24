(ns tests.macros
  (:require #?(:clj [cljs.core]))
  #?(:cljs (:require-macros [tests.macros])))

(defmacro slurpit [path]
  (clojure.core/slurp path))
