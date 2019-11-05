(ns tests.smart-contracts-test)
  (def smart-contracts
    {:my-contract {:name "MyContract" :address "0x279a693aef4722be8e1f568bcf044e95a48a7431"} :forwarder {:name "Forwarder" :address "0x1403b58ae47071230a39689d614e66f49aa2f7d3" :forwards-to :my-contract}})
