(ns browser-test.all
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :as t]
 #_           [tests.macros :refer [slurpit]]
            [cljs-web3-next.core :as web3-core]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs-web3-next.helpers :as web3-helpers]
            [cljs-web3-next.personal :as web3-personal]
  #_          [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [oops.core :refer [ocall oget oset! oapply+]]
            [cljs.core.async :refer [<!]]
#_            [tests.smart-contracts-test :refer [smart-contracts]]
            [district.shared.async-helpers :as async-helpers]))

(async-helpers/extend-promises-as-channels!)

#_(def abi (oget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "abi"))

(def w3 (web3-core/create-web3 "ws://127.0.0.1:9545"))
(def gas-limit 4500000)

(def contract-source "
  pragma solidity ^0.4.6;

  contract test {
    function multiply(uint a) returns(uint d) {
      return a * 7;
    }
  }")

(deftest basic
  (is (web3-core/connected? w3))
  (is (string? (web3-core/version-api w3)))
  #_ (is (string? (web3/version-ethereum w3)))              ; Not working with testrpc
  (is (seq (web3-eth/accounts w3)))
  ;; (is (= (web3/sha3 "1") "0xc89efdaa54c0f20c7adf612882df0950f5a951637e0307cdcb4c672f298b8bc6"))
  ;; (is (= (web3/to-hex js/Web3 "A") "0x41"))
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

  (web3-eth/set-default-account! w3 (first (web3-eth/accounts w3)))
  (is (= (web3-eth/default-account w3) (first (web3-eth/accounts w3))))

  #_(is (web3-eth/default-block w3))
  #_(is (web3-eth/syncing? w3))

  #_(is (web3-eth/coinbase w3))
  #_(is (number? (web3-eth/hashrate w3)))

  #_(is (web3-net/listening? w3))
  #_(is (number? (web3-net/peer-count w3)))

  #_(is (number? (.toNumber (web3-eth/gas-price w3))))
  #_(is (number? (.toNumber (web3-eth/get-balance w3 (web3-eth/coinbase w3)))))

  #_(is (map? (web3-eth/get-block w3 "latest")))
  #_(is (seq (web3-eth/get-compilers w3)))

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
