(ns cljs-web3-next.net
  (:require [cljs-web3.utils :as u]))

;;TODO convert to multi-arity as nodejs env can't fallback on injected web3

(def listening?

  "This property is read only and says whether the node is actively
  listening for network connections or not.

  Parameters:
  callback - callback with two parameters: error and result

  Returns true if the client is actively listening for network connections,
  otherwise false.

  Example:
  user> `(listening? (fn [err res] (when-not err (println res))))`
  nil
  user> true"
  (u/prop-or-clb-fn "net" "isListening"))


(def peer-count
  "This property is read only and returns the number of connected peers.

  Returns the number of peers currently connected to the client.

  Example:
  user> `(peer-count (fn [err res] (when-not err (println res))))`
  nil
  user> 4"
  (u/prop-or-clb-fn "net" "getPeerCount"))
