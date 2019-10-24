(ns macros
  (:require #?(:clj [cljs.core]))
  #?(:cljs (:require-macros [macros])))

(defmacro slurpit [path]
  (clojure.core/slurp path))
