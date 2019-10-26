(ns tests.nodejs-runner
  (:require
    [cljs.nodejs :as nodejs]
    [doo.runner :refer-macros [doo-tests]]
    [tests.web3-tests]))

(nodejs/enable-util-print!)

(doo-tests 'tests.web3-tests)
