(ns memefactory.shared.smart-contracts-test)
  (def smart-contracts
    {:my-contract {:name "MyContract" :address "0x8190d10c57da0b48acf120e4840a7a068388f9e1"} :forwarder {:name "Forwarder" :address "0x06a673506bd8ba27974c6f74410cac8d2477afd4" :forwards-to :my-contract}})
