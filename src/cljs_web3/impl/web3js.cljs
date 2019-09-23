(ns web3.impl.web3js
  (:require [cljs-web3.api :refer [Web3Api]]
            [cljs-web3.utils :as web3-utils]
            [cljs-web3.macros :refer [defrecord+]]
            [cljs.nodejs :as nodejs]))

(def Web3 (nodejs/require "web3"))

;; TODO : clojurize all return values
(defrecord+ Web3Js []
  Web3Api
  (-http-provider [_ uri]
    (new Web3 (new (aget Web3 "providers" "HttpProvider") uri)))
  (-websocket-provider [_ uri]
    (new Web3 (new (aget Web3 "providers" "WebsocketProvider") uri)))
  (-is-listening? [_ provider]
    (js-invoke (aget provider "eth" "net") "isListening"))
  (-sha3 [_ provider arg]
    (js-invoke (aget provider "utils") "sha3" arg))
  (-solidity-sha3 [_ provider args]
    (js-invoke (aget provider "utils") "soliditySha3" args))
  (-contract-at [_ provider abi address]
    (new (aget provider "eth" "Contract") abi address))
  (-get-transaction-receipt [_ provider tx-hash]
    (js-invoke (aget provider "eth") "getTransactionReceipt" tx-hash))
  (-accounts [_ provider]
    (js-invoke (aget provider "eth") "getAccounts"))
  (-get-block-number [_ provider]
    (js-invoke (aget provider "eth") "getBlockNumber"))
  (-get-block [_ provider block-hash-or-number return-transactions? & [callback]]
    (js-invoke (aget provider "eth") "getBlock" block-hash-or-number return-transactions? callback))
  (-contract-call [_ contract-instance method args opts]
    (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-utils/camel-case (name method)) args) "call" (clj->js opts)))
  (-contract-send [_ contract-instance method args opts]
    (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-utils/camel-case (name method)) args) "send" (clj->js opts)))
  (-subscribe-logs [_ provider contract-instance opts & [callback]]
    (js-invoke (aget provider "eth") "subscribe" "logs" (web3-utils/cljkk->js opts) callback))
  (-decode-log [_ provider abi data topics]
    (js-invoke (aget provider "eth" "abi") "decodeLog" (clj->js abi) data (clj->js topics)))
  (-on [_ event-emitter evt callback]
    (js-invoke event-emitter "on" (name evt) callback))
  (-unsubscribe [_ subscription]
    (js-invoke subscription "unsubscribe"))
  (-clear-subscriptions [_ provider]
    (js-invoke (aget provider "eth") "clearSubscriptions"))
  (-get-past-events [this contract-instance event opts & [callback]]
    (js-invoke contract-instance "getPastEvents" (web3-utils/camel-case (name event)) (web3-utils/cljkk->js opts) callback))


  )

(defn new []
  (->Web3Js))
