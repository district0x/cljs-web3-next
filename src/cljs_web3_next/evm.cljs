(ns cljs-web3-next.evm
  "Functions that can be used to control testrpc behaviour. Can ONLY (!) be used
  with testrpc.

  See https://github.com/ethereumjs/testrpc for more information."
  (:require [cljs-web3-next.utils :refer [callback-js->clj js->cljkk]]))


(defn- jsonrpc-callback->clj
  [callback]
  (callback-js->clj (fn [err res]
    (cond
      err (callback err res)
      (:error res) (callback (:error res) res)
      (:result res) (callback nil (:result res))
      :default (callback err res)))))

(defn send-jsonrpc
  "Sends a jsonrpc message to the currentProvider.

  Parameter:
  web3 - web3 instance

  Example:
  user> `(send-async-fn web3)`
  #object..."
  [web3 jsonrpc & [callback]]
    (js-invoke
           (aget web3 "currentProvider")
           "send" jsonrpc (if callback (jsonrpc-callback->clj callback) (fn [_err _res]))))


(defn- increase-time-jsonrpc [args]
  (clj->js {:jsonrpc "2.0"
            :method "evm_increaseTime"
            :params args
            :id (.getTime (js/Date.))}))

(defn increase-time!
  "Jump forward in time in the EVM.

  Parameters:
  web3     - web3 instance
  args     - The amount of time to increase in seconds.
  callback - callback with two parameters, error and result.

  Results is the total time adjustment, in seconds.

  Example:
  user> `(web3-evm/increase-time! web3 [1000] callback)`"
  [web3 args & [callback]]
  (send-jsonrpc web3 (increase-time-jsonrpc args) callback))


(defn- mine-block-jsonrpc []
  (clj->js {:jsonrpc "2.0"
            :method "evm_mine"
            :params []
            :id (.getTime (js/Date.))}))

(defn mine-block!
  "Force a block to be mined. Mines a block independent of
  whether or not mining is started or stopped.

  Parameters:
  web3     - web3 instance
  callback - callback with two parameters, error and result.

  Example:
  user> `(web3-evm/mine! web3 callback)`"
  [web3 & [callback]]
  (send-jsonrpc web3 (mine-block-jsonrpc) callback))


(defn- revert-jsonrpc [args]
  (clj->js {:jsonrpc "2.0"
            :method "evm_revert"
            :params args
            :id (.getTime (js/Date.))}))

(defn revert!
  "Revert the state of the blockchain to a previous snapshot.

  Takes a single
  parameter, which is the snapshot id to revert to. If no snapshot id is passed
  it will revert to the latest snapshot. Returns true.

  Parameters:
  web3     - web3 instance
  args     - snapshot id to revert to, if no snapshot id is passed, it will
             revert to the latest snapshot
  callback - callback with two parameters, error and result.

  Result is true.

  Example:
  user> `(web3-evm/revert! web3 [0] callback)`"
  [web3 args & [callback]]
  (send-jsonrpc web3 (revert-jsonrpc args) callback))


(defn- snapshot-jsonrpc []
  (clj->js {:jsonrpc "2.0"
            :method "evm_snapshot"
            :params []
            :id (.getTime (js/Date.))}))

(defn snapshot!
  "Snapshot the state of the blockchain at the current block.

  Parameters:
  web3     - web3 instance
  callback - callback with two parameters, error and result.

  Result is the id of the snapshot created.

  Example:
  user> `(web3-evm/snapshot! web3 callback)`
  "
  [web3 & [callback]]
  (send-jsonrpc web3 (snapshot-jsonrpc) callback))
