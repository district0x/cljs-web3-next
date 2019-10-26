(ns tests.nodejs-runner
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.test :refer [run-tests]]
    [tests.web3-tests]))

(nodejs/enable-util-print!)

(run-tests 'tests.web3-tests)

(set! *main-cli-fn* -main)
