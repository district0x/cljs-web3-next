(ns cljs-web3-next.evm
  (:require [oops.core :refer [ocall oget]]))

(defn increase-time [provider seconds]
  (ocall (oget provider "evm") "increaseTime" seconds))

(defn mine-block [provider]
  (ocall (oget provider "evm") "mineBlock"))

(defn snapshot [provider]
  (ocall (oget provider "evm") "snapshot"))

(defn revert [provider]
  (ocall (oget provider "evm") "revert"))
