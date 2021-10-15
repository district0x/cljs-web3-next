(ns tests.all
  (:require
    [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
    [day8.re-frame.test :refer [run-test-async wait-for run-test-sync]]
    [district.ui.smart-contracts.deploy-events :as deploy-events]
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
