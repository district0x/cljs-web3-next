(ns cljs-web3.api
  (:require [cljs-web3.macros :refer [defprotocol+]]))

(defprotocol+ Web3Api
  (-http-provider [this uri])
  (-websocket-provider [this uri])
  (-sha3 [this provider arg])
  (-solidity-sha3 [this provider args])
  (-contract-at [this provider abi address])
  (-get-transaction-receipt [this provider tx-hash])
  (-accounts [this provider])
  (-get-block-number [this provider])
  (-get-block [this provider block-hash-or-number return-transactions? & [callback]])
  (-contract-call [this contract-instance method args opts])
  (-contract-send [this contract-instance method args opts])
  (-subscribe-logs [this provider contract-instance opts & [callback]])
  (-decode-log [this provider abi data topics])
  (-on [this event-emitter k callback])
  (-unsubscribe [this subscription])
  (-clear-subscriptions [this provider])
  (-get-past-events [this contract-instance event opts & [callback]])

  )
