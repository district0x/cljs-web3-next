(ns tests.all
  (:require
    [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
    [day8.re-frame.test :refer [run-test-async wait-for run-test-sync]]
    [district.ui.smart-contracts.deploy-events :as deploy-events]
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

(def abi (oget (js/JSON.parse (slurpit "./resources/public/contracts/build/MyContract.json")) "abi"))

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
          contract2 (subscribe [::subs/contract :contract2])

          contract1-addr (subscribe [::subs/contract-address :contract1])
          contract2-addr (subscribe [::subs/contract-address :contract2])

          contract1-abi (subscribe [::subs/contract-abi :contract1])
          contract2-abi (subscribe [::subs/contract-abi :contract2])

          contract1-bin (subscribe [::subs/contract-bin :contract1])
          contract2-bin (subscribe [::subs/contract-bin :contract2])

          contract1-instance (subscribe [::subs/instance :contract1])
          contract2-instance (subscribe [::subs/instance :contract2])

          contract1-name (subscribe [::subs/contract-name :contract1])
          contract2-name (subscribe [::subs/contract-name :contract2])]

      (-> (mount/with-args
            {:web3 {:url "https://mainnet.infura.io/ "}
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

        (is (= (-> @contracts :contract2 :name)
               (-> smart-contracts :contract2 :name)
               (:name @contract2)
               @contract2-name))

        (is (= (-> @contracts :contract1 :address)
               (-> smart-contracts :contract1 :address)
               (:address @contract1)
               @contract1-addr
               (aget @contract1-instance "address")))

        (is (= (-> @contracts :contract2 :address)
               (-> smart-contracts :contract2 :address)
               (:address @contract2)
               @contract2-addr
               (aget @contract2-instance "address")))

        (is (= (js/JSON.stringify (-> @contracts :contract1 :abi))
               (responses "./Contract1.abi")
               (js/JSON.stringify (:abi @contract1))
               (js/JSON.stringify @contract1-abi)
               (js/JSON.stringify (aget @contract1-instance "abi"))))

        (is (= (js/JSON.stringify (-> @contracts :contract2 :abi))
               (responses "./Contract2.abi")
               (js/JSON.stringify (:abi @contract2))
               (js/JSON.stringify @contract2-abi)
               (js/JSON.stringify (aget @contract2-instance "abi"))))

        (is (= (-> @contracts :contract1 :bin)
               (str "0x" (responses "./Contract1.bin"))
               (:bin @contract1)
               @contract1-bin))

        (is (= (-> @contracts :contract2 :bin)
               (str "0x" (responses "./Contract2.bin"))
               (:bin @contract2)
               @contract2-bin))))))

(deftest tests2
  (run-test-sync
    (let [contracts (subscribe [::subs/contracts])
          contract1-bin (subscribe [::subs/contract-bin :contract1])
          contract1-abi (subscribe [::subs/contract-abi :contract1])]
      (-> (mount/with-args
            {:web3 {:url "https://mainnet.infura.io/ "}
             :web3-accounts {:disable-loading-at-start? true}
             :smart-contracts {:contracts {}
                               :contracts-path "./"
                               :disable-loading-at-start? true}})
        (mount/start))

      (is (= {} @contracts))

      (dispatch [::events/load-contracts {:contracts (select-keys smart-contracts [:contract1])
                                          :contracts-path "./"}])

      (is (= (js/JSON.stringify (-> @contracts :contract1 :abi))
             (responses "./Contract1.abi")
             (js/JSON.stringify @contract1-abi)))

      (testing "Doesn't load bin unless explicitly told"
        (is (nil? @contract1-bin))))))


(deftest deploy-tests
  (run-test-async
    (let [contract-address (subscribe [::subs/contract-address :deploy-test-contract])
          contract-instance (subscribe [::subs/instance :deploy-test-contract])
          active-account (subscribe [::accounts-subs/active-account])]

      (-> (mount/with-args
            {:web3 {:url "http://localhost:8549"}
             :smart-contracts {:contracts smart-contracts
                               :disable-loading-at-start? true}})
        (mount/start))


      (is (nil? @contract-address))
      (is (nil? @contract-instance))

      (wait-for [::accounts-events/active-account-changed ::accounts-events/accounts-load-failed]
        (dispatch [::deploy-events/deploy-contract :deploy-test-contract {:arguments [1]
                                                                          :from @active-account}])

        (wait-for [::events/set-contract ::deploy-events/contract-deploy-failed]
          (is (web3/address? @contract-address))
          (is (not (nil? @contract-instance))))))))
