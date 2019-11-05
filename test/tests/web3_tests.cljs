(ns tests.web3-tests
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :as t]
            [tests.macros :refer [slurpit]]
            [cljs-web3.core :as web3-core]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [cljs-web3.helpers :as web3-helpers]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [cljs.core.async :refer [<!]]
            [tests.smart-contracts-test :refer [smart-contracts]]
            [district.shared.async-helpers :as async-helpers]))

(async-helpers/extend-promises-as-channels!)

(def abi (aget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "abi"))

(deftest test-web3 []
  (let [web3 (web3-core/websocket-provider "ws://127.0.0.1:8545")]
    (async done
           (go
             (let [connected? (<! (web3-eth/is-listening? web3))
                   accounts (<! (web3-eth/accounts web3))
                   block-number (<! (web3-eth/get-block-number web3))
                   block (js->clj (<! (web3-eth/get-block web3 block-number false)) :keywordize-keys true)
                   address (-> smart-contracts :my-contract :address)
                   my-contract (web3-eth/contract-at web3 abi address)
                   event-interface (web3-helpers/event-interface my-contract :SetCounterEvent)
                   event-emitter (-> (web3-eth/subscribe-events web3
                                                                my-contract
                                                                :SetCounterEvent
                                                                {:from-block (inc block-number)}
                                                                (fn [_ event]
                                                                  (let [return-values (aget event "returnValues")
                                                                        evt-values (web3-helpers/return-values->clj return-values event-interface)]
                                                                    (is (= "3" (:new-value evt-values))))))
                                     (#(web3-eth/on web3 % :connected (fn [sub-id]
                                                                        (is (int? sub-id)))))
                                     (#(web3-eth/on web3 % :data (fn [event]
                                                                   (is (= "3" (:new-value (web3-helpers/return-values->clj (aget event "returnValues") event-interface))))))))
                   event-signature (:signature event-interface)
                   event-log-emitter (web3-eth/subscribe-logs web3
                                                              {:address [address]
                                                               :topics [event-signature]
                                                               :from-block (inc block-number)}
                                                              (fn [_ event]
                                                                (let [return-values (web3-eth/decode-log web3 (:inputs event-interface) (aget event "data") [event-signature])
                                                                      evt-values (web3-helpers/return-values->clj return-values event-interface)]
                                                                  (is (= "3" (:new-value evt-values))))))
                   tx (<! (web3-eth/contract-send web3
                                                  my-contract
                                                  :set-counter
                                                  [3]
                                                  {:from (first accounts)
                                                   :gas 4000000}))
                   seven (<! (web3-eth/contract-call web3
                                                     my-contract
                                                     :my-plus
                                                     [3 4]
                                                     {:from (first accounts)}))
                   tx-receipt (<! (web3-eth/get-transaction-receipt web3 (aget tx "transactionHash")))
                   past-events (<! (web3-eth/get-past-events web3
                                                             my-contract
                                                             :SetCounterEvent
                                                             {:from-block 0
                                                              :to-block "latest"}))
                   past-logs (<! (web3-eth/get-past-logs web3
                                                         {:address [address]
                                                          :topics [event-signature]
                                                          :from-block 0
                                                          :to-block "latest"}))]

               (is (= "7" seven))
               (is (= "0x8bb5d9c30000000000000000000000000000000000000000000000000000000000000003" (web3-eth/encode-abi web3 my-contract :set-counter [3])))
               (is (= address (string/lower-case (aget my-contract "_address"))))
               (is (aget tx-receipt "status"))
               (is connected?)
               (is (= 10 (count accounts)))
               (is (int? block-number))
               (is (map? block))
               (is (= "3" (:new-value (web3-helpers/return-values->clj (aget past-events "0" "returnValues") event-interface))))
               (is (= 1 (count past-logs)))

               (web3-eth/unsubscribe web3 event-emitter)
               (web3-eth/unsubscribe web3 event-log-emitter)
               (web3-core/disconnect web3)

               (is (= "0xf652222313e28459528d920b65115c16c04f3efc82aaedc97be59f3f377c0d3f"
                      (web3-utils/solidity-sha3 web3 6)))

               (is (= "0x34ee2a785aa8c43ab6ddf4bd8cd55e2cf7ee009305e966472ee22a637a2bb71f"
                      (web3-utils/solidity-sha3 web3 "0x7d10b16dd1f9e0df45976d402879fb496c114936")))

               (is (= "0x4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45"
                      (web3-utils/solidity-sha3 web3 "abc")))

               (is (= "0x789357bc7419b62048fc1339ce448db0836603d3c0738082337b68e2b17d26a6"
                      (web3-utils/solidity-sha3 web3 "0x7d10b16dd1f9e0df45976d402879fb496c114936" 6 "abc")))

               (done))))))
