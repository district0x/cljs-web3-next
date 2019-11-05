(ns cljs-web3.evm)

(defn increase-time [provider seconds]
  (js-invoke (aget provider "evm") "increaseTime" seconds))

(defn mine-block [provider]
  (js-invoke (aget provider "evm") "mineBlock"))
