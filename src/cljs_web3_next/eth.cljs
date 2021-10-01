(ns cljs-web3-next.eth
  (:require [cljs-web3-next.helpers :as web3-helpers]
            [oops.core :refer [ocall oget oset!]]))

(defn is-listening? [provider & [callback]]
  (ocall provider "eth" "net" "isListening" (remove nil? [callback])))

(defn contract-at [provider abi address]
  (new (oget provider "eth" "Contract") abi address))

(defn get-transaction-receipt [provider tx-hash & [callback]]
  (ocall provider "eth" "getTransactionReceipt" (remove nil? [tx-hash callback])))

(defn accounts [provider]
  (ocall provider "eth" "getAccounts"))

(defn get-balance [provider address]
  (ocall provider "eth" "getBalance" address))

(defn get-block-number [provider & [callback]]
  (ocall provider "eth" "getBlockNumber" (remove nil? [callback])))

(defn get-block [provider block-hash-or-number return-transactions? & [callback]]
  (ocall provider "eth" "getBlock" (remove nil? [block-hash-or-number return-transactions? callback])))

(defn encode-abi [contract-instance method args]
  (ocall contract-instance "methods" (web3-helpers/camel-case (name method)) (clj->js args) "encodeABI"))

(defn contract-call [contract-instance method args opts]
  (ocall contract-instance "methods" (web3-helpers/camel-case (name method)) (clj->js args) "call" (clj->js opts)))

(defn contract-send [contract-instance method args opts]
  (ocall contract-instance "methods" (web3-helpers/camel-case (name method)) (clj->js args) "send" (clj->js opts)))

(defn subscribe-events [contract-instance event opts & [callback]]
  (ocall contract-instance "events" (web3-helpers/camel-case (name event)) (remove nil? [(web3-helpers/cljkk->js opts) callback])))

(defn subscribe-logs [provider opts & [callback]]
  (ocall provider "eth" "subscribe" "logs" (web3-helpers/cljkk->js opts) callback))

(defn decode-log [provider abi data topics]
  (ocall provider "eth" "abi" "decodeLog" (clj->js abi) data (clj->js topics)))

(defn unsubscribe [subscription & [callback]]
  (ocall subscription "unsubscribe" callback))

(defn clear-subscriptions [provider]
  (ocall provider "eth" "clearSubscriptions"))

(defn get-past-events [contract-instance event opts & [callback]]
  (ocall contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))

(defn get-past-logs [provider opts & [callback]]
  (ocall provider "eth" "getPastLogs" (web3-helpers/cljkk->js opts) callback))

(defn on [event-emitter event callback]
  (ocall event-emitter "on" (name event) callback))

(defn eth
  "Gets eth object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [provider]
  (oget provider "eth"))


;; legacy


(defn default-account
  "Gets the default address that is used for the following methods (optionally
  you can overwrite it by specifying the :from key in their options map):

  - `send-transaction!`
  - `call!`

  Parameters:
  web3 - web3 instance

  Returns the default address HEX string.

  Example:
  user> `(default-account web3-instance)`
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider]
  (oget provider "eth" "defaultAccount"))

(defn set-default-account!
  "Sets the default address that is used for the following methods (optionally
  you can overwrite it by specifying the :from key in their options map):

  - `send-transaction!`
  - `call!`

  Parameters:
  web3    - web3 instance
  hex-str - Any 20 bytes address you own, or where you have the private key for


  Returns a 20 bytes HEX string representing the currently set address.

  Example:
  user> (set-default-account! web3-instance
                              \"0x85d85715218895ae964a750d9a92f13a8951de3d\")
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider hex-str]
  (oset! provider "eth" "defaultAccount" hex-str))

(defn default-block
  "This default block is used for the following methods (optionally you can
  override it by passing the default-block parameter):

  - `get-balance`
  - `get-code`
  - `get-transactionCount`
  - `get-storageAt`
  - `call`
  - `contract-call`
  - `estimate-gas`

  Parameters:
  web3 - web3 instance

  Returns one of:
  - a block number
  - \"earliest\", the genisis block
  - \"latest\", the latest block (current head of the blockchain)
  - \"pending\", the currently mined block (including pending transactions)

  Example:
  user> `(default-block web3-instance)`
  \"latest\""
  [provider]
  (oget provider "eth" "defaultBlock"))


(defn set-default-block!
  "Sets default block that is used for the following methods (optionally you can
  override it by passing the default-block parameter):

  - `get-balance`
  - `get-code`
  - `get-transactionCount`
  - `get-storageAt`
  - `call`
  - `contract-call`
  - `estimate-gas`

  Parameters:
  web3  - web3 instance
  block - one of:
            - a block number
            - \"earliest\", the genisis block
            - \"latest\", the latest block (current head of the blockchain)
            - \"pending\", the currently mined block (including pending
              transactions)

  Example:
  user> `(set-default-block! web3-instance \"earliest\")`
  \"earliest\""
  [provider block]
  (oset! provider "eth" "defaultBlock" block))

;; recheck currying here
(defn stop-watching!
  "Stops and uninstalls the filter.

  Arguments:
  filter - the filter to stop"
  [filter & args]
  (unsubscribe filter (first args)))


(defn contract-get-data
  "Gets binary data of a contract method call.

  Use the kebab-cases version of the original method.
  E.g., function fooBar() can be addressed with :foo-bar.

  Parameters:
  contract-instance - an instance of the contract (obtained via `contract` or
                      `contract-at`)
  method            - the kebab-cased version of the method
  args              - arguments to the method

  Example:
  user> `(web3-eth/contract-call ContractInstance :multiply 5)`
  25"
  [contract-instance method & args]
  (let [method-name (web3-helpers/camel-case (name method))
        method-fn (oget contract-instance method-name)]
    (if method-fn
      (oget method-fn "getData" args)
      (throw (str "Method: " method-name " was not found in object.")))))


;; DEPRECATED
(defn get-compile
  "Gets compile object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [web3]
  nil)