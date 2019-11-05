(ns cljs-web3.evm)

(defn increase-time [{:keys [:provider]} seconds]
  (js-invoke (aget provider "evm") "increaseTime" seconds))

(defn mine-block [{:keys [:provider]}]
  (js-invoke (aget provider "evm") "mineBlock"))
