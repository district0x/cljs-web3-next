(ns tests.web3-tests
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :as t]
            [tests.macros :refer [slurpit]]
            [cljs-web3-next.core :as web3-core]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs-web3-next.helpers :as web3-helpers]
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
                   event-emitter (-> (web3-eth/subscribe-events my-contract
                                                                :SetCounterEvent
                                                                {:from-block (inc block-number)}
                                                                (fn [_ event]
                                                                  (let [return-values (aget event "returnValues")
                                                                        evt-values (web3-helpers/return-values->clj return-values event-interface)]
                                                                    (is (= "3" (:new-value evt-values))))))
                                     (#(web3-eth/on % :connected (fn [sub-id]
                                                                        (is (int? sub-id)))))
                                     (#(web3-eth/on % :data (fn [event]
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
                   tx (<! (web3-eth/contract-send my-contract
                                                  :set-counter
                                                  [3]
                                                  {:from (first accounts)
                                                   :gas 4000000}))
                   seven (<! (web3-eth/contract-call my-contract
                                                     :my-plus
                                                     [3 4]
                                                     {:from (first accounts)}))
                   tx-receipt (<! (web3-eth/get-transaction-receipt web3 (aget tx "transactionHash")))
                   past-events (<! (web3-eth/get-past-events my-contract
                                                             :SetCounterEvent
                                                             {:from-block 0
                                                              :to-block "latest"}))
                   past-logs (<! (web3-eth/get-past-logs web3
                                                         {:address [address]
                                                          :topics [event-signature]
                                                          :from-block 0
                                                          :to-block "latest"}))]

               (is (= "7" seven))
               (is (= "0x8bb5d9c30000000000000000000000000000000000000000000000000000000000000003" (web3-eth/encode-abi my-contract :set-counter [3])))
               (is (= address (string/lower-case (aget my-contract "_address"))))
               (is (aget tx-receipt "status"))
               (is connected?)
               (is (= 10 (count accounts)))
               (is (int? block-number))
               (is (map? block))
               (is (= "3" (:new-value (web3-helpers/return-values->clj (aget past-events "0" "returnValues") event-interface))))
               (is (= 1 (count past-logs)))

               (web3-eth/unsubscribe event-emitter)
               (web3-eth/unsubscribe event-log-emitter)
               (web3-core/disconnect web3)

               (is (= "0xf652222313e28459528d920b65115c16c04f3efc82aaedc97be59f3f377c0d3f"
                      (web3-utils/solidity-sha3 web3 6)))

               (is (= "0x34ee2a785aa8c43ab6ddf4bd8cd55e2cf7ee009305e966472ee22a637a2bb71f"
                      (web3-utils/solidity-sha3 web3 "0x7d10b16dd1f9e0df45976d402879fb496c114936")))

               (is (= "0x4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45"
                      (web3-utils/solidity-sha3 web3 "abc")))

               (is (= "0x789357bc7419b62048fc1339ce448db0836603d3c0738082337b68e2b17d26a6"
                      (web3-utils/solidity-sha3 web3 "0x7d10b16dd1f9e0df45976d402879fb496c114936" 6 "abc")))

               (is (= "0x00DC857B6f66bf96154FF4541E4a2Fe87E3db6fc"
                      (web3-utils/address->checksum web3 "0x00dc857b6f66bf96154ff4541e4a2fe87e3db6fc")))

               (done))))))

(deftest legacy
  "0.* web3 backward compatibility test"
  []
  (let [web3 (web3-core/websocket-provider "ws://127.0.0.1:8545")]
    (async done
           (go (is (<! (web3-core/connected? web3)))
               (is (string? (<! (web3-core/version-api web3))))
               (is (= (<! (web3-core/sha3 "1")) "0xc89efdaa54c0f20c7adf612882df0950f5a951637e0307cdcb4c672f298b8bc6"))
               (is (= (<! (web3-core/to-hex web3 "A")) "0x41"))

               (done)
               )))
  #_ (is (string? (web3/version-ethereum w3)))              ; Not working with testrpc
  ;; (is (seq (web3-eth/accounts w3)))
  ;; (is (= (web3/to-ascii "0x41") "A"))
  ;; (is (= (web3/from-ascii "A") "0x41"))
  ;; (is (= (web3/to-decimal "0xFF") 255))
  ;; (is (= (web3/from-decimal 255) "0xff"))
  ;; (is (= (web3/from-wei 1000000000000000000 :ether) "1"))
  ;; (is (= (web3/to-wei 1 :ether) "1000000000000000000"))
  ;; (is (= (web3/pad-left "1" 5 "A") "AAAA1"))
  ;; (is (= (web3/pad-right "1" 5 "A") "1AAAA"))
  ;; (is (.eq (web3/to-big-number 1) 1))
  ;; (is (web3/address? "0x6fce64667819c82a8bcbb78e294d7b444d2e1a29"))
  ;; (is (not (web3/address? "0x6fce64667819c82a8bcbb78e294d7b444d2e1a294")))
  ;; (is (web3/current-provider w3))
  ;; (is (= (web3-settings/default-block w3) "latest"))

  ;; (web3-eth/set-default-account! w3 (first (web3-eth/accounts w3)))
  ;; (is (= (web3-eth/default-account w3) (first (web3-eth/accounts w3))))

  ;; (is (web3-eth/default-block w3))
  ;; (is (web3-eth/syncing? w3))

  ;; (is (web3-eth/coinbase w3))
  ;; (is (number? (web3-eth/hashrate w3)))

  ;; (is (web3-net/listening? w3))
  ;; (is (number? (web3-net/peer-count w3)))

  ;; (is (number? (.toNumber (web3-eth/gas-price w3))))
  ;; (is (number? (.toNumber (web3-eth/get-balance w3 (web3-eth/coinbase w3)))))

  ;; (is (map? (web3-eth/get-block w3 "latest")))
  ;; (is (seq (web3-eth/get-compilers w3)))

  #_ (is (web3-personal/unlock-account w3 (web3-eth/default-account w3) "m" 999999))

  #_ (let [create-contract-ch (chan)]
    (async done
      (let [compiled (web3-eth/compile-solidity w3 contract-source)]
        (is (map? compiled))
        (is (number? (web3-eth/estimate-gas w3 compiled)))
        (web3-eth/contract-new
          w3
          (:abi-definition (:info compiled))
          {:data (:code compiled)
           :gas gas-limit
           :from (first (web3-eth/accounts w3))}
          #(go (>! create-contract-ch [%1 %2]))))

      (go
        (let [[err Contract] (<! create-contract-ch)]
          (is (not err))
          (is Contract)
          (is (not (:address Contract)))
          (is (map? (web3-eth/get-transaction w3 (aget Contract "transactionHash")))))

        (let [[err Contract] (<! create-contract-ch)]
          (is (not err))
          (is (aget Contract "address"))
          (is (string? (web3-eth/contract-call Contract :multiply 5)))))
      (done))))
