(ns cljs-web3-next.eth
  (:require [cljs-web3-next.helpers :as web3-helpers]
            [oops.core :refer [ocall ocall+ oget oget+ oset! oapply+]]))

(defn is-listening? [provider & [callback]]
  (oapply+ provider "eth.net.isListening" (remove nil? [callback])))

(defn contract-at [provider abi address]
  (new (aget provider "eth" "Contract") abi address))

(defn get-transaction-receipt [provider tx-hash & [callback]]
  (oapply+ provider "eth.getTransactionReceipt" (remove nil? [tx-hash callback])))

(defn accounts [provider]
  (js-invoke (oget provider "eth") "getAccounts"))

;; recheck conflicts with updated fn
;; (defn get-balance [provider address]
;;   (ocall provider "eth" "getBalance" address))

(defn get-block-number [provider & [callback]]
  (oapply+ provider "eth.getBlockNumber" (remove nil? [callback])))

(defn get-block [provider block-hash-or-number return-transactions? & [callback]]
  (oapply+ provider "eth.getBlock" (remove nil? [block-hash-or-number return-transactions? callback])))

;; not working
(defn encode-abi [contract-instance method args]
  (js-invoke (oapply+ (oget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "encodeABI"))

(defn contract-call [contract-instance method args opts]
  (ocall (oapply+ (oget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "call" (clj->js opts)))

(defn contract-send [contract-instance method args opts]
  (ocall (oapply+ (oget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "send" (clj->js opts)))

(defn subscribe-events [contract-instance event opts & [callback]]
  (ocall+ (oget contract-instance "events") (web3-helpers/camel-case (name event)) (remove nil? [(web3-helpers/cljkk->js opts) callback])))

(defn subscribe-logs [provider opts & [callback]]
  (js-invoke (aget provider "eth") "subscribe" "logs" (web3-helpers/cljkk->js opts) callback))

(defn decode-log [provider abi data topics]
  (ocall+ provider "eth.abi.decodeLog" (clj->js abi) data (clj->js topics)))

(defn unsubscribe [subscription & [callback]]
  (ocall subscription "unsubscribe" callback))

(defn clear-subscriptions [provider]
  (ocall provider "eth" "clearSubscriptions"))

(defn get-past-events [contract-instance event opts & [callback]]
  (ocall contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))

(defn get-past-logs [provider opts & [callback]]
  (js-invoke (oget provider "eth") "getPastLogs" (web3-helpers/cljkk->js opts) callback))

;; (defn get-past-events [contract-instance event opts & [callback]]
;;   (js-invoke contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))

;; (defn get-past-logs [provider opts & [callback]]
;;   (js-invoke (aget provider "eth") "getPastLogs" (web3-helpers/cljkk->js opts) callback))

(defn on [event-emitter event callback]
  (ocall event-emitter "on" (name event) callback))

(defn eth
  "Gets eth object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [provider]
  (oget provider "eth"))

(defn iban
  "Gets iban object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [provider]
  (oget provider "Iban"))


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

;;DEPRECATED partially
(defn syncing
  "This property is read only and returns the either a sync object, when the
  node is syncing or false.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns a sync object as follows, when the node is currently syncing or false:
  - startingBlock: The block number where the sync started.
  - currentBlock:  The block number where at which block the node currently
                   synced to already.
  - highestBlock:  The estimated block number to sync to.

  Example:
  user> `(syncing web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> `false`"
  [provider]
  (ocall provider "eth" "isSyncing"))

(def syncing? syncing)

(defn coinbase
  "This property is read only and returns the coinbase address where the mining
  rewards go to.

  Parameters:
  web3 - web3 instance

  Returns a string representing the coinbase address of the client.

  Example:
  user> `(coinbase web3-instance)`
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider]
  (ocall provider "eth" "getCoinbase"))

(defn mining?
  "This property is read only and says whether the node is mining or not.

  Parameters:
  web3 - web3 instance

  Returns a boolean: true if the client is mining, otherwise false.

  Example:
  `(mining? web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> `false`"
  [provider]
  (ocall provider "eth" "isMining"))


(defn hashrate
  "This property is read only and returns the number of hashes per second that
  the node is mining with.

  Parameters:
  web3 - web3 instance

  Returns a number representing the hashes per second.

  user> `(hashrate web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> 0
  "
  [provider]
  (ocall provider "eth" "getHashrate"))


(defn gas-price
  "This property is read only and returns the current gas price. The gas price
  is determined by the x latest blocks median gas price.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns a BigNumber instance of the current gas price in wei.

  Example:
  user> `(gas-price web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> #object[e 90000000000]"
  [provider]
  (ocall provider "eth" "getGasPrice"))

(defn block-number
  "This property is read only and returns the current block number.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns the number of the most recent block.

  Example:
  `(block-number web3-instance
                 (fn [err res] (when-not err (println res))))`
  nil
  user> `1783426`"
  [provider]
  (get-block-number provider))

(defn get-balance
  "Get the balance of an address at a given block.

  Parameters:
  web3          - web3 instance
  address       - The address to get the balance of.
  default-block - If you pass this parameter it will not use the default block
                  set with set-default-block.
  callback-fn   - callback with two parameters, error and result

  Returns a BigNumber instance of the current balance for the given address in
  wei.

  Example:
  user> `(get-balance web3-instance
                      \"0x85d85715218895ae964a750d9a92f13a8951de3d\"
                      \"latest\"
                      (fn [err res] (when-not err (println res))))`
  nil
  user> #object[e 1729597111000000000]"
  [provider & [address default-block :as args]]
  (oapply+ provider "eth" "getBalance" args))

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
        method-fn (oget+ contract-instance method-name)]
    (if method-fn
      (ocall method-fn "getData" args)
      (throw (str "Method: " method-name " was not found in object.")))))


;; DEPRECATED
;; these functions existed in 0.* but
;; were interfaced to empty implementations/broken/soft-deprecated
(defn get-compile
  "Gets compile object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [web3]
  nil)

(defn namereg
  "Returns GlobalRegistrar object.

  See https://github.com/ethereum/web3.js/blob/master/example/namereg.html
  for an example in JavaScript."
  [web3]
  nil)

(defn get-compilers
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & args]
  nil)


(defn compile-solidity
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)


(defn compile-lll
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)


(defn compile-serpent
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)

(defn register
  "(Not Implemented yet) Registers the given address to be included in
  `accounts`. This allows non-private-key owned accounts to be associated
  as an owned account (e.g., contract wallets).

  Parameters:
  web3        - web3 instance
  address     - string representing the address
  callback-fn - callback with two parameters, error and result."
  [web3 address]
  nil)


(defn unregister
  "(Not Implemented yet) Unregisters a given address.

  Parameters:
  web3        - web3 instance
  address     - string representing the address
  callback-fn - callback with two parameters, error and result."
  [web3 address]
  nil)

(defn get-storage-at
  "Get the storage at a specific position of an address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the storage from.
  position      - The index position of the storage.
  default-block - If you pass this parameter it will not use the default block
                  set with web3.eth.defaultBlock.
  callback-fn   - callback with two parameters, error and result

  Returns the value in storage at the given position.

  Example:
  user> `(get-storage-at web3-instance
                         \"0x85d85715218895ae964a750d9a92f13a8951de3d\"
                         0
                         \"latest\"
                         (fn [err res] (when-not err (println res))))`
  nil
  user> \"0x0000000000000000000000000000000000000000000000000000000000000000\" "
  [web3 & [address position default-block :as args]]
  (oapply+ (oget web3 "eth") "getStorageAt" args))

(defn get-code
  "Get the code at a specific address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the code from.
  default-block - If you pass this parameter it will not use the default block set
                  with `get-default-block!`.
  callback-fn   - callback with two parameters, error and result

  Returns the data at given address HEX string.

  Example:
  user> (get-code web3-instance
                  \"0x85d85715218895ae964a750d9a92f13a8951de3d
                  0
                  \"latest\"
                  (fn [err res] (when-not err (println res))))
  nil
  user> `0x`
  "
  [web3 & [address default-block :as args]]
  (oapply+ (eth web3) "getCode" args))

(defn get-block-transaction-count
  "Returns the number of transaction in a given block.

  Parameters
  web3                 - web3 instance
  block-hash-or-number - The block number or hash. Or the string \"earliest\",
                         \"latest\" or \"pending\" as in the default block
                         parameter.
  callback-fn          - callback with two parameters, error and result

  Example:
  user> `(get-block-transaction-count
           web3-instance
           0
           (fn [err res] (when-not err (println res))))`
  nil
  user> 0"
  [web3 & [block-hash-or-number :as args]]
  (oapply+ (eth web3) "getBlockTransactionCount" args))

(defn get-uncle
  "Returns a blocks uncle by a given uncle index position.
  Parameters

  Parameters:
  web3                        - web3 instance
  block-hash-or-number        - The block number or hash. Or the string
                                \"earliest\", \"latest\" or \"pending\" as in
                                the default block parameter
  uncle-number                - The index position of the uncle
  return-transaction-objects? - If true, the returned block will contain all
                                transactions as objects, if false it will only
                                contains the transaction hashes
  default-block               - If you pass this parameter it will not use the
                                default block set with (set-default-block)
  callback-fn                 - callback with two parameters, error and result

  Returns the returned uncle. For a return value see `(get-block)`.

  Note: An uncle doesn't contain individual transactions."
  [web3 & [block-hash-or-number uncle-number return-transaction-objects? :as args]]
  (oapply+ (eth web3) "getUncle" args))

(defn get-transaction
 "Returns a transaction matching the given transaction hash.

  Parameters:
  web3             - web3 instance
  transaction-hash - The transaction hash.
  callback-fn      - callback with two parameters, error and result

  Returns a transaction object its hash transaction-hash:

  - hash: String, 32 Bytes - hash of the transaction.
  - nonce: Number - the number of transactions made by the sender prior to this
    one.
  - block-hash: String, 32 Bytes - hash of the block where this transaction was
                                   in. null when its pending.
  - block-number: Number - block number where this transaction was in. null when
                           its pending.
  - transaction-index: Number - integer of the transactions index position in the
                                block. null when its pending.
  - from: String, 20 Bytes - address of the sender.
  - to: String, 20 Bytes - address of the receiver. null when its a contract
                           creation transaction.
  - value: BigNumber - value transferred in Wei.
  - gas-price: BigNumber - gas price provided by the sender in Wei.
  - gas: Number - gas provided by the sender.
  - input: String - the data sent along with the transaction.

  Example:
  user> `(get-transaction
           web3-instance
           \"0x...\"
           (fn [err res] (when-not err (println res))))`
  nil
  user> {:r 0x...
         :v 0x2a
         :hash 0xf...
         :transaction-index 3 ...
         (...)
         :to 0x...}"
  [web3 & [transaction-hash :as args]]
  (oapply+ (eth web3) "getTransaction" args))

(defn get-transaction-from-block
  "Returns a transaction based on a block hash or number and the transactions
  index position.

  Parameters:
  web3                 - web3 instance
  block-hash-or-number - A block number or hash. Or the string \"earliest\",
                         \"latest\" or \"pending\" as in the default block
                         parameter.
  index                - The transactions index position.
  callback-fn          - callback with two parameters, error and result
  Number               - The transactions index position.

  Returns a transaction object, see `(get-transaction)`

  Example:
  user> `(get-transaction-from-block
           web3-instance
           1799402
           0
           (fn [err res] (when-not err (println res))))`
  nil
  user> {:r 0x...
         :v 0x2a
         :hash 0xf...
         :transaction-index 0 ...
         (...)
         :to 0x...}"
  [web3 & [block-hash-or-number index :as args]]
  (oapply+ (eth web3) "getTransactionFromBlock" args))

(defn get-transaction-count
  "Get the numbers of transactions sent from this address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the numbers of transactions from.
  default-block - If you pass this parameter it will not use the default block
                  set with set-default-block.
  callback-fn   - callback with two parameters, error and result

  Returns the number of transactions sent from the given address.

  Example:
  user> `(get-transaction-count web3-instance \"0x8\"
           (fn [err res] (when-not err (println res))))`
  nil
  user> 16"
  [web3 & [address default-block :as args]]
  (oapply+ (eth web3) "getTransactionCount" args))

(defn contract
  "Important - callback has been deprecated
  Creates an *abstract* contract object for a solidity contract, which can be used to
  initiate contracts on an address.

  Parameters:
  web3          - web3 instance
  abi           - ABI array with descriptions of functions and events of
                  the contract

  Returns a contract object."
  [web3 & [abi :as args]]
  (new (aget web3 "eth" "Contract") abi))

(defn contract-new
  "Deploy a contract asynchronous from a Solidity file.

  Parameters:
  web3             - web3 instance
  abi              - ABI array with descriptions of functions and events of
                     the contract
  transaction-data - map that contains
    - :gas - max gas to use
    - :data the BIN of the contract
    - :from account to use
  callback-fn      - callback with two parameters, error and contract.
                     From the contract the \"address\" property can be used to
                     obtain the address. And the \"transactionHash\" to obtain
                     the hash of the transaction, which created the contract.

  Example:
  `(contract-new web3-instance
                 abi
                 {:from \"0x..\"
                  :data bin
                  :gas  4000000}
                 (fn [err contract]
                   (if-not err
                    (let [address (aget contract \"address\")
                          tx-hash (aget contract \"transactionHash\")]
                      ;; Two calls: transaction received
                      ;; and contract deployed.
                      ;; Check address on the second call
                      (when (address? address)
                        (do-something-with-contract contract)
                        (do-something-with-address address)))
                    (println \"error deploying contract\" err))))`
   nil"
  [web3 abi & [transaction-data callback-fn :as args]]
  (oapply+ (contract web3 abi) "deploy" args))

(defn send-transaction!
  "Sends a transaction to the network.

  Parameters:
  web3               - web3 instance
  transaction-object - The transaction object to send:

    :from: String - The address for the sending account. Uses the
                    `default-account` property, if not specified.

    :to: String   - (optional) The destination address of the message, left
                               undefined for a contract-creation
                               transaction.

    :value        - (optional) The value transferred for the transaction in
                               Wei, also the endowment if it's a
                               contract-creation transaction.

    :gas:         - (optional, default: To-Be-Determined) The amount of gas
                    to use for the transaction (unused gas is refunded).
    :gas-price:   - (optional, default: To-Be-Determined) The price of gas
                    for this transaction in wei, defaults to the mean network
                    gas price.
    :data:        - (optional) Either a byte string containing the associated
                    data of the message, or in the case of a contract-creation
                    transaction, the initialisation code.
    :nonce:       - (optional) Integer of a nonce. This allows to overwrite your
                               own pending transactions that use the same nonce.
  callback-fn   - callback with two parameters, error and result, where result
                  is the transaction hash

  Returns the 32 Bytes transaction hash as HEX string.

  If the transaction was a contract creation use `(get-transaction-receipt)` to
  get the contract address, after the transaction was mined.

  Example:
  user> (send-transaction! web3-instance {:to \"0x..\"}
          (fn [err res] (when-not err (println res))))
  nil
  user> 0x..."
  [web3 & [transaction-object :as args]]
  (oapply+ (eth web3) "sendTransaction" args))


(defn send-raw-transaction!
  "Sends an already signed transaction. For example can be signed using:
  https://github.com/SilentCicero/ethereumjs-accounts

  Parameters:
  web3                    - web3 instance
  signed-transaction-data - Signed transaction data in HEX format

  callback-fn             - callback with two parameters, error and result

  Returns the 32 Bytes transaction hash as HEX string.

  If the transaction was a contract creation use `(get-transaction-receipt)`
  to get the contract address, after the transaction was mined.

  See https://github.com/ethereum/wiki/wiki/JavaScript-API#example-46 for a
  JavaScript example."
  [web3 & [signed-transaction-data :as args]]
  (oapply+ (eth web3) "sendSignedTransaction" args))

(def send-signed-transaction send-raw-transaction!)

(defn send-iban-transaction!
  "Sends IBAN transaction from user account to destination IBAN address.

  note: IBAN protocol seems to be soft-deprecated

  Parameters:
  web3          - web3 instance
  from          - address from which we want to send transaction
  iban-address  - IBAN address to which we want to send transaction
  value         - value that we want to send in IBAN transaction
  callback-fn   - callback with two parameters, error and result

  Note: uses smart contract to transfer money to IBAN account.

  Example:
  user> `(send-iban-transaction! '0xx'
                                 'NL88YADYA02'
                                  0x100
                                  (fn [err res] (prn res)))`"
  [web3 & [from iban-address value cb :as args]]
  (oapply+ (eth web3) "sendTransaction" [from (ocall (iban web3) "toAddress" iban-address) value cb]))
