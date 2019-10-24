(ns tests.web3-tests
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :as t]
            [cljs-web3.macros]
            [cljs-web3.core :as web3-core]
            [cljs-web3.eth :as web3-eth]
            [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [<!] :as async]
            [district.shared.async-helpers :as async-helpers]
            [web3.impl.web3js :as web3js]))

(async-helpers/extend-promises-as-channels!)
(nodejs/enable-util-print!)

(deftest test-web3 []
  (let [inst (web3js/new)
        web3 {:instance inst
              :provider (web3-core/websocket-provider inst "ws://127.0.0.1:8549")}]
    (async done
           (go
             (let [connected? (<! (web3-eth/is-listening? web3))
                   accounts (<! (web3-eth/accounts web3))
                   block-number (<! (web3-eth/get-block-number web3))
                   block (js->clj (<! (web3-eth/get-block web3 block-number false)) :keywordize-keys true)]

               (is connected?)
               (is (= 10 (count accounts)))
               (is (int? block-number))
               (is (map? block))

               (web3-core/disconnect web3)
               (done))))))
