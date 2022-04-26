(ns tests.smart-contracts-test)

(def smart-contracts
  {:my-contract {:name "MyContract" :address "0x555384605D2B6c71bE931B6e649baE1c4981AAad"} :forwarder {:name "Forwarder" :address "0xbdd338A8C6b2b3A7F1439f67fdfDbfa914170A74" :forwards-to :my-contract}})
