(ns browser-test.all
  (:require
    [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
    [day8.re-frame.test :refer [run-test-async wait-for run-test-sync]]
    [cljs-web3-next.core :as web3-core]
    [cljs.core.async :refer [<!]]
    [district.ui.smart-contracts.events :as events]
    [district.ui.smart-contracts.subs :as subs]
    [district.ui.smart-contracts]
    [district.ui.web3-accounts.events :as accounts-events]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-accounts]
    [tests.macros :refer [slurpit]]
    [oops.core :refer [ocall oget oset! oapply+]]
    [district.shared.async-helpers :as async-helpers]
    [mount.core :as mount]
    [re-frame.core :refer [reg-event-fx dispatch-sync subscribe reg-cofx reg-fx dispatch]]
    [cljs-web3.core :as web3]))

(async-helpers/extend-promises-as-channels!)

(def cabi (oget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "abi"))

(def cbin (oget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "bin"))

(def responses
  {"./Contract1.abi" cabi
   "./Contract1.bin" cbin})

(def smart-contracts
  {:contract1 {:name "Contract1" :address "0xfbb1b73c4f0bda4f67dca266ce6ef42f520fbb98"}
   ;; irrelevant
   :deploy-test-contract {:name "DeployTestContract"
                          :abi (js/JSON.parse "[{\"inputs\":[{\"name\":\"someNumber\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]")
                          :bin "0x60606040523415600e57600080fd5b604051602080607183398101604052808051915050801515602e57600080fd5b50603580603c6000396000f3006060604052600080fd00a165627a7a72305820f6c231e485f5b65831c99412cbcad5b4e41a4b69d40f3d4db8de3a38137701fb0029"}})

(reg-fx
  :http-xhrio
  (fn [requests]
    (doseq [{:keys [:uri :on-success]} requests]
      (dispatch (vec (concat on-success [(responses uri)]))))))


(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})


(deftest tests
  (run-test-async
    (let [contracts (subscribe [::subs/contracts])
          contract1 (subscribe [::subs/contract :contract1])
          contract1-addr (subscribe [::subs/contract-address :contract1])
          contract1-abi (subscribe [::subs/contract-abi :contract1])
          contract1-bin (subscribe [::subs/contract-bin :contract1])
          contract1-instance (subscribe [::subs/instance :contract1])
          contract1-name (subscribe [::subs/contract-name :contract1])]

      (-> (mount/with-args
            {:web3 {:url "http://127.0.0.1:8545"}
             :web3-accounts {:disable-loading-at-start? true}
             :smart-contracts {:contracts smart-contracts
                               :load-bin? true
                               :contracts-path "./"}})
        (mount/start))

      (wait-for [::events/contracts-loaded ::events/contract-load-failed]
        (is (= (-> @contracts :contract1 :name)
               (-> smart-contracts :contract1 :name)
               (:name @contract1)
               @contract1-name))

        (is (= (-> @contracts :contract1 :address)
               (-> smart-contracts :contract1 :address)
               (:address @contract1)
               @contract1-addr
               (aget @contract1-instance "address")))

        (is (= (js/JSON.stringify (-> @contracts :contract1 :abi))
               (responses "./Contract1.abi")
               (js/JSON.stringify (:abi @contract1))
               (js/JSON.stringify @contract1-abi)
               (js/JSON.stringify (aget @contract1-instance "abi"))))

        (is (= (-> @contracts :contract1 :bin)
               (str "0x" (responses "./Contract1.bin"))
               (:bin @contract1)
               @contract1-bin))
        ;; testing default-web3 here, web3 should be injected already
        (is (= (<! (web3-core/sha3  "1"))
               "0xc89efdaa54c0f20c7adf612882df0950f5a951637e0307cdcb4c672f298b8bc6"))
        ))))

#_(deftest legacy
  "0.* web3 backward compatibility test"
  []
  ;; test it out with http provider
  (let [web3 (web3-core/http-provider "http://127.0.0.1:9545")]
    (async done
           (go (is (<! (web3-core/connected? web3)))
               #_(is (string? (<! (web3-core/version-api web3))))
               (is (= (<! (web3-core/sha3 web3 "1")) "0xc89efdaa54c0f20c7adf612882df0950f5a951637e0307cdcb4c672f298b8bc6"))
               (is (= (<! (web3-core/to-hex web3 "A")) "0x41"))
               (is (= (<! (web3-core/to-ascii web3 "0x41")) "A"))
               (is (= (<! (web3-core/from-ascii web3 "A")) "0x41"))
               (is (= (<! (web3-core/to-decimal web3 "0xFF")) 255))
               (is (= (<! (web3-core/from-decimal web3 255)) "0xff"))
               (is (= (<! (web3-core/from-wei web3 1000000000000000000 :ether)) "1"))
               (is (= (<! (web3-core/to-wei web3 1 :ether)) "1000000000000000000"))
               (is (<! (web3-personal/unlock-account web3 (web3-eth/default-account web3) "m" 999999)))

               (done)
               )))
  #_ (is (string? (web3/version-ethereum w3)))              ; Not working with testrpc
  ;; (is (seq (web3-eth/accounts w3)))
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
