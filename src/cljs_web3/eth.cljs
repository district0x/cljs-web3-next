(ns cljs-web3.eth
  (:require [cljs-web3.helpers :as web3-helpers]))

(defn is-listening? [{:keys [:provider]} & [callback]]
  (apply js-invoke (aget provider "eth" "net") "isListening" (remove nil? [callback])))

(defn contract-at [{:keys [:provider]} abi address]
  (new (aget provider "eth" "Contract") abi address))

(defn get-transaction-receipt [{:keys [:provider]} tx-hash & [callback]]
  (apply js-invoke (aget provider "eth") "getTransactionReceipt" (remove nil? [tx-hash callback])))

(defn accounts [{:keys [:provider]}]
  (js-invoke (aget provider "eth") "getAccounts"))

(defn get-block-number [{:keys [:provider]} & [callback]]
  (apply js-invoke (aget provider "eth") "getBlockNumber" (remove nil? [callback])))

(defn get-block [{:keys [:provider]} block-hash-or-number return-transactions? & [callback]]
  (apply js-invoke (aget provider "eth") "getBlock" (remove nil? [block-hash-or-number return-transactions? callback])))

(defn encode-abi [contract-instance method args]
  (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "encodeABI"))

(defn contract-call [contract-instance method args opts]
  (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "call" (clj->js opts)))

(defn contract-send [contract-instance method args opts]
  (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "send" (clj->js opts)))

(defn subscribe-events [contract-instance event opts & [callback]]
  (apply js-invoke (aget contract-instance "events") (web3-helpers/camel-case (name event)) (remove nil? [(web3-helpers/cljkk->js opts) callback])))

(defn subscribe-logs [{:keys [:provider]} opts & [callback]]
  (js-invoke (aget provider "eth") "subscribe" "logs" (web3-helpers/cljkk->js opts) callback))

(defn decode-log [{:keys [:provider]} abi data topics]
  (js-invoke (aget provider "eth" "abi") "decodeLog" (clj->js abi) data (clj->js topics)))

(defn unsubscribe [subscription & [callback]]
  (js-invoke subscription "unsubscribe" callback))

(defn clear-subscriptions [{:keys [:provider]}]
  (js-invoke (aget provider "eth") "clearSubscriptions"))

(defn get-past-events [contract-instance event opts & [callback]]
  (js-invoke contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))

(defn get-past-logs [{:keys [:provider]} opts & [callback]]
  (js-invoke (aget provider "eth") "getPastLogs" (web3-helpers/cljkk->js opts) callback))

(defn on [event-emitter event callback]
  (js-invoke event-emitter "on" (name event) callback))
