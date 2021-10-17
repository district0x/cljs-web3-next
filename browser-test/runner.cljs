(ns browser-test.runner
  (:require
    [cljs.spec.alpha :as s]
    [doo.runner :refer-macros [doo-tests]]
    [browser-test.all]))

(s/check-asserts true)

(enable-console-print!)

(doo-tests 'browser-test.all)
