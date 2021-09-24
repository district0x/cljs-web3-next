(ns tests.smart-contracts-test)

(def smart-contracts
  {:my-contract {:name "MyContract" :address "0x4DDDC882943dbf39CAC88daf01dC084ee65F1Ddd"} :forwarder {:name "Forwarder" :address "0xcEbe4c7537dd5c469c2486C9930A9ee3b98EDd33" :forwards-to :my-contract}})
