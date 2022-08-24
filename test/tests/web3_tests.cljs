(ns tests.web3-tests
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :as t]
            [tests.macros :refer [slurpit]]
            [cljs-web3-next.core :as web3-core]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.evm :as web3-evm]
            [cljs-web3-next.utils :as web3-utils]
            [cljs-web3-next.helpers :as web3-helpers]
            [cljs-web3-next.personal :as web3-personal]
            ; [cljs.nodejs :as nodejs] ; caused loading error `ReferenceError: require is not defined`
            [clojure.string :as string]
            [oops.core :refer [ocall oget oset! oapply+]]
            [cljs.core.async :refer [<! chan put! timeout]]
            [tests.smart-contracts-test :refer [smart-contracts]]
            [district.shared.async-helpers :as async-helpers]
            ["web3" :as Web3]))

(async-helpers/extend-promises-as-channels!)

(def abi (oget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "abi"))

(defn running-in-browser? []
  (and (exists? js/window) (exists? js/document))) ; Based on https://github.com/flexdinesh/browser-or-node/blob/master/src/index.js#L2

(defn get-web3-provider [in-browser?]
  (if in-browser?
(web3-core/ws-provider "ws://localhost:8549")
    (web3-core/ws-provider "ws://localhost:8549")))

(deftest test-web3 []
  (let [provider (get-web3-provider (running-in-browser?))
        web3 (new Web3 provider)]
    (async done
           (go
             (let [connected? (<! (web3-eth/is-listening? web3))
                   accounts (<! (web3-eth/accounts web3))
                   block-number (<! (web3-eth/get-block-number web3))
                   chain-id (<! (web3-eth/get-chain-id web3))
                   block (js->clj (<! (web3-eth/get-block web3 block-number false)) :keywordize-keys true)
                   address (-> smart-contracts :my-contract :address)
                   my-contract (web3-eth/contract-at web3 abi address)
                   event-interface (web3-helpers/event-interface my-contract :SetCounterEvent)

                   event-emitter (web3-eth/subscribe-events my-contract :SetCounterEvent {:from-block (inc block-number)})
                   connected-chan (timeout 1000)
                   _ (web3-eth/on event-emitter :connected (fn [sub-id] (put! connected-chan sub-id) ))
                   _ (is (not (nil? (<! connected-chan))))

                   _ (web3-eth/on event-emitter :data (fn [event]
                                                        (is (= "3" (:new-value (web3-helpers/return-values->clj (aget event "returnValues") event-interface))))))

                   event-signature (:signature event-interface)
                   initial-logs (<! (web3-eth/get-past-logs web3 {:address [address] :topics [event-signature] :from-block block-number :to-block "latest"}))
                   event-log-emitter (web3-eth/subscribe-logs
                                      web3
                                      {:address [address]
                                       :topics [event-signature]
                                       :from-block (inc block-number)}
                                      (fn [_ event]
                                        (let [return-values (web3-eth/decode-log web3 (:inputs event-interface) (oget event "data") [event-signature])
                                              evt-values (web3-helpers/return-values->clj return-values event-interface)]
                                          (is (= "3" (:new-value evt-values))))))
                   tx (<! (web3-eth/contract-send my-contract :set-counter [3] {:from (first accounts) :gas 4000000}))
                   _ (is (not (= (type tx) js/Error)) tx)
                   seven (<! (web3-eth/contract-call my-contract :my-plus [3 4] {:from (first accounts)}))
                   tx-receipt (<! (web3-eth/get-transaction-receipt web3 (aget tx "transactionHash")))
                   past-events (<! (web3-eth/get-past-events my-contract :SetCounterEvent {:from-block 0 :to-block "latest"}))
                   final-logs (<! (web3-eth/get-past-logs web3 {:address [address] :topics [event-signature] :from-block block-number :to-block "latest"}))
                   new-logs (- (count final-logs) (count initial-logs))]


               (is (= "7" seven))
               (is (= "1337" (str chain-id)))
               (is (= "0x8bb5d9c30000000000000000000000000000000000000000000000000000000000000003" (web3-eth/encode-abi my-contract :set-counter [3])))
               (is (= (string/lower-case address) (string/lower-case (aget my-contract "_address"))))
               (is (aget tx-receipt "status"))
               (is connected?)
               (is (= 10 (count accounts)))
               (is (int? block-number))
               (is (map? block))
               (is (= "3" (:new-value (web3-helpers/return-values->clj (aget past-events "0" "returnValues") event-interface))))
               (is (= 1 new-logs))

               (web3-eth/unsubscribe event-emitter)
               (web3-eth/unsubscribe event-log-emitter)
               (web3-core/disconnect web3)

               (is (some? (web3-core/on-connect web3 identity)) "Adding connect listener must succeed")
               (is (some? (web3-core/on-disconnect web3 identity)) "Adding disconnect listener must succeed")
               (is (some? (web3-core/on-error web3 identity)) "Adding error listener must succeed (does nothing)")

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


(deftest test-web3-utils
  []
  (is (= (web3-core/sha3 "Some string to be hashed")
          "0xed973b234cf2238052c9ac87072c71bcf33abc1bbd721018e0cca448ef79b379"))
  (is (= (web3-core/to-hex "foo") "0x666f6f"))
  (is (= (web3-core/to-ascii "0x666f6f") "foo"))
  (is (= (web3-core/from-ascii "ethereum") "0x657468657265756d"))
  (is (= (web3-core/to-decimal "0x15") 21))
  (is (= (web3-core/from-decimal 21) "0x15"))
  (is (= (web3-core/from-wei "10" :ether) "0.00000000000000001"))
  (is (= (web3-core/to-wei "10" :ether) "10000000000000000000"))
  (is (= (str (web3-core/to-big-number "10000000000000000000")) "10000000000000000000"))
  (is (= (web3-core/pad-left "foo" 8) "00000foo"))
  (is (= (web3-core/pad-right "foo" 8 "b") "foobbbbb"))
  (is (web3-core/address? "0x8888f1f195afa192cfee860698584c030f4c9db1"))
  (is (not (web3-core/address? "0x8888F1f195afa192cfee860698584c030f4c9db1")))

  (let [provider (get-web3-provider (running-in-browser?))
     web3 (new Web3 provider)]
    (is (= (web3-utils/solidity-sha3 web3 "Some string to be hashed")
          "0xed973b234cf2238052c9ac87072c71bcf33abc1bbd721018e0cca448ef79b379"))

    (is (= (web3-utils/address->checksum web3 "0x8888f1f195afa192cfee860698584c030f4c9db1")
          "0x8888f1F195AFa192CfeE860698584c030f4c9dB1"))))


(deftest test-web3-evm
  []
  (let [provider (get-web3-provider (running-in-browser?))
        web3 (new Web3 provider)]

    (async done
      (go
        (let [block-number (<! (web3-eth/get-block-number web3))
              block (js->clj (<! (web3-eth/get-block web3 block-number false)) :keywordize-keys true)]
          (web3-evm/snapshot! web3
            (fn [_err res]
              (let [snapshot-id res]
                (web3-evm/increase-time! web3 [3600]
                  (fn [_err res]
                    (is (>= res 3600))
                    (web3-evm/mine-block! web3
                      (fn [_err _res]
                        (go
                          (let [new-block-number (<! (web3-eth/get-block-number web3))
                                new-block (js->clj (<! (web3-eth/get-block web3 new-block-number false)) :keywordize-keys true)]
                            (is (= (inc block-number) new-block-number))
                            (is (<= (+ 3600 (:timestamp block)) (:timestamp new-block)))
                            (web3-evm/revert! web3 [snapshot-id]
                              (fn [_err _res]
                                (go
                                  (let [new-block-number (<! (web3-eth/get-block-number web3))
                                       new-block (js->clj (<! (web3-eth/get-block web3 new-block-number false)) :keywordize-keys true)]
                                    (is (= block-number new-block-number))
                                    (is (= (:timestamp block) (:timestamp new-block)))
                                    (done)))))))))))))))))))
