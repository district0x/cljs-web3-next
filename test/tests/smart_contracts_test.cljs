(ns tests.smart-contracts-test)
  (def smart-contracts
    {:my-contract {:name "MyContract" :address "0x0000000000000000000000000000000000000000"}
     :forwarder {:name "Forwarder" :address "0x0000000000000000000000000000000000000000" :forwards-to :my-contract}})
