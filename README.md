# cljs-web3-next

[![CircleCI](https://circleci.com/gh/district0x/cljs-web3-next/tree/master.svg?style=svg)](https://circleci.com/gh/district0x/cljs-web3-next/tree/master)

This ClojureScript library provides a API for interacting with [Ethereum](https://www.ethereum.org/) nodes.
It uses a [bridge pattern](https://en.wikipedia.org/wiki/Bridge_pattern) to decouple its API from the subsequent implementations, allowing the latter to vary at runtime.

In practice what this means is that future versions of the library will maintain the same function signatures and behaviour, it also means that anyone can provide a suitable implementation in this library, or opt-in to use the API in own.

## Installation
Latest released version of this library: <br>
[![Clojars Project](https://img.shields.io/clojars/v/cljs-web3-next.svg)](https://clojars.org/cljs-web3-next)

## API Overview

- [cljs-web3.api](#api)
- [cljs-web3.core](#core)
  - [http-provider](#http-provider)
  - [websocket-provider](#websocket-provider)
  - [extend](#extend)
  - [connection-url](#connection-url)
  - [connected?](#connected?)
  - [disconnect](#disconnect)
  - [on-connect](#on-connect)
  - [on-disconnect](#on-disconnect)
  - [on-error](#on-error)
- [cljs-web3.eth](#eth)
  - [is-listening?](#is-listening?)
  - [contract-at](#contract-at)
  - [get-transaction-receipt](#get-transaction-receipt)
  - [accounts](#accounts)
  - [get-block-number](#get-block-number)
  - [get-block](#get-block)
  - [encode-abi](#encode-abi)
  - [contract-call](#contract-call)
  - [contract-send](#contract-send)
  - [subscribe-events](#subscribe-events)
  - [subscribe-logs](#subscribe-logs)
  - [decode-log](#decode-log)
  - [unsubscribe](#unsubscribe)
  - [clear-subscriptions](#clear-subscriptions)
  - [get-past-events](#get-past-events)
  - [get-past-logs](#get-past-logs)
  - [on](#on)
- [cljs-web3.utils](#utils)
  - [sha3](#sha3)
  - [solidity-sha3](#solidity-sha3)
  - [from-ascii](#from-ascii)
  - [to-ascii](#to-ascii)
  - [number-to-hex](#number-to-hex)
  - [from-wei](#from-wei)
  - [to-wei](#to-wei)
  - [address?](#address?)
- [cljs-web3.evm](#evm)
  - [increase-time](#increase-time)
  - [mine-block](#mine-block)
- [cljs-web3.helpers](#helpers)
  - [js->cljkk](#js->cljkk)
  - [cljkk->js](#cljkk->js)
  - [event-interface](#event-interface)
  - [return-values->clj](#return-values->clj)

### <a name="api"></a>`cljs-web3.api`

This namespace contains the API of this library which the participating implementations need to implement.

### <a name="core"></a>`cljs-web3.core`

Core functions which deal with creating and checking the status of Web3 connections.

#### <a name="websocket-provider" >`websocket-provider`

This function is the gateway to using the library.
Most other functions will take the map it returns as their first argument, unless specified otherwise.

It creates a Web3 instance over a websocket connection.
Takes an instance of the [Web3Api](#api) protocol and the url as parameters.
Returns a map (the provider map) with two keys:
- `:instance` : the instance of the implementation of the API, same as you have passed it
- `:provider` : the websocket

For example to use the Web3JS implementation:

```clojure
(ns my-district
  (:require [cljs-web3.core :as web3-core]
            [web3.impl.web3js :as web3js]))

(def web3 (web3-core/websocket-provider (web3js/new) "ws://127.0.0.1:8545"))
```

#### <a name="http-provider">`http-provider`

Similar to [websocket-provider](#websocket-provider), but creates a Web3 instance from a URL address.

*NOTE* some functions, notably subscriptions (see e.g. [subscribe-events](#subscribe-events)), will work differently with than `http-provider`, which is why it's generally recommended to use the [websocket](#websocket-provider) connections for your subscriptions.

#### <a name="extend">`extend`

Allows for extending the Web3 object with any supported [JSON RPC](https://github.com/ethereum/wiki/wiki/JSON-RPC#json-rpc-methods) method, which is otherwise not a part of this library.
Takes as arguments:
- a map returned by the [websocket-provider](#websocket-provider) or [http-provider](#http-provider) function.
- the module name (a keyword)
- a colection of `method` maps with following keys:
  - `name` : Name of the method to add
  - `call` : The RPC method name
  - `params` : The number of parameters for that call (optional)

Example:

```clojure
(extend web3
 :evm
 [{:name "increaseTime"
   :call "evm_increaseTime"
   :params 1})])
```

Returns the same web3 map as passed, but now the provider is extended with the [`increaseTime`]() method in the `evm` module, which you can invoke like this:

```clojure
(.increaseTime (aget web3 :provider "evm") 1000)
```

#### <a name="connection-url" >`connection-url`

Takes as arguments a map returned by the [provider](#websocket-provider) function and returns the URL of the node it is connected to.

```clojure
(connection-url web3)
;; "ws://127.0.0.1:8545"
```

#### <a name="connected?">`connected?`

Takes as arguments a map returned by the [provider](#websocket-provider) function and returns the connection status as a boolean value.
This function is synchronous, for an asynchronous method see [is-listening?](@is-listening?).

#### <a name="disconnect">`disconnect`

Immediately disconnects the [provider](#websocket-provider), returns `nil`.

```clojure
(disconnect web3)
```

#### <a name="on-connect">`on-connect`

Takes a [provider](#websocket-provider) map and a callback function as arguments, callback is executed when the connection is established.

```clojure
(web3-core/on-connect web3 (fn [event] (prn "just connected")))
```

#### <a name="on-disconnect">`on-disconnect`

Takes a [provider](#websocket-provider) map and a callback as arguments, callback is executed when the connection is dropped.

```clojure
(web3-core/on-disconnect web3 (fn [event] (prn "your web3 socket has lost its connection")))
```

#### <a name="on-error">`on-error`

Similar as [on-connect](#on-connect) and [on-disconnect](#on-disconnect) but executes the callback when connection throws an error.

### <a name="eth" >`cljs-web3.eth`

This namespace contains functions for interacting with the Ethereum blockchain and Ethereum smart contracts.

#### <a name= "is-listening?">`is-listening?`

Asynchronous version of the [connected?](#connected?) function, takes the [provider](#websocket-provider) map and a callback function.
Returns a JS/Promise which returns a boolean.

You can use it to set a periodically executing connection healthcheck:

```clojure
(js/setInterval (fn []
                  (is-listening? web3
                                 (fn [_ connected?]
                                   (when-not connected?
                                     (reset-connection)))))
                3000)
```

#### <a name= "contract-at">`contract-at`

Takes a provider map, contract [abi](https://solidity.readthedocs.io/en/v0.5.3/abi-spec.html#abi-json) interface as returned by the [solc](https://solidity.readthedocs.io/en/v0.5.3/using-the-compiler.html#compiler-input-and-output-json-description) compiler (in a JSON format).
Returns a Contract instance.

```
(def abi (aget (.readFileSync js/fs MyContract.json) "abi))

(contract-at web3 abi "0x98f93ed24052ceed35741beee1d75287cb297137")
```

#### <a name= "get-transaction-receipt">`get-transaction-receipt`
Takes a provider map, transaction hash (a String) and an optional callback function.
Returns a JS/Promise which evaluates to the receipt of that transaction.

```clojure
(get-transaction-receipt web3 "0xd5000d0de00e50d6ac47fe48d11d9dc8258e74fc69b57b5c804e7ddc424af8d0" (fn [tx] (prn "I got the receipt" tx))
```

#### <a name= "accounts">`accounts`

Returns the availiable ethereum accounts in the wallet on the node it is connected to.

```
(accounts web3)
```

#### <a name= "get-block-number">`get-block-number`

Takes a web3 map and an optional callback as arguments.
Returns a JS/Promise which evaluates to the current block number.

```
(get-block-number web3)
```

#### <a name= "get-block">`get-block`

Takes as arguments:

- a web3 map
- a block number or hash
- a boolean parameter specifying whether to include the transactions
- an optional callback

Returns a JS/Promise which evaluates to the representation of the last mined block.

```
(get-block web3 block-number false)
```

#### <a name= "encode-abi">`encode-abi`

This function returns the ABI bytecode of a transaction.

It takes as arguments:
- the [provider](#websocket-provider) map
- smart contract instance, as returned by the [contract-at](#contract-at) function
- the kebab-cased keyword with the name of the smart contracts function
- a vector with the arguments of the function

Returns bytecode as a string.

```
(encode-abi web3 my-contract :set-counter [3])
```

#### <a name= "contract-call">`contract-call`

This function executes a statless (read only) method of a smart contract.

It takes as arguments:
- the [provider](#websocket-provider) map
- smart contract instance, as returned by the [contract-at](#contract-at) function
- the kebab-cased keyword with the name of the smart contracts function to execute
- a vector with the arguments of the function
- map of options:
 - `:from` : the account calling the contract (see [accounts](#accounts))

Returns a JS/Promise which evaluates to the return value of that function.

```
(web3-eth/contract-call web3
                        my-contract
                        :my-plus
                        [3 4]
                        {:from (first accounts)})
```

#### <a name= "contract-send">`contract-send`

This function executes a state-altering (payable) method of a smart contract.

It takes as arguments:
- the [provider](#websocket-provider) map
- smart contract instance, as returned by the [contract-at](#contract-at) function
- the kebab-cased keyword with the name of the smart contracts function to execute
- a vector with the arguments of the function
- map of options:
 - `:from` : the account calling the contract (see [accounts](#accounts))
 - `:gas` : max amount of gas you are willing to spend

Returns a JS/Promise which evaluates to the tx-hash once the transaction is mined.

```
(<! (web3-eth/contract-send web3
                            my-contract
                            :set-counter
                            [3]
                            {:from (first accounts)
                             :gas 4000000}))
```

#### <a name= "subscribe-events">`subscribe-events`

Creates a subscription listening to a specific contracts event.

It takes as arguments:
- the [provider](#websocket-provider) map
- smart contract instance, as returned by the [contract-at](#contract-at) function
- the CamelCased keyword with the name of the smart contracts event to listen to
- map of options:
  - `:from-block` : the block number (greater than or equal to) from which to get events on.
- a nodejs-style callback function (`error` is a first parameters and `response` the second), executed each time the event is seen (optional)

Returns a `EventEmitter` object which can be subsequently used with [on](#on) to react to an even finer-grained control.

```
(web3-eth/subscribe-events web3
                           my-contract
                           :SetCounterEvent
                           {:from-block block-number}
                           (fn [error event]
                             (prn "new event" event})))
```

#### <a name= "subscribe-logs">`subscribe-logs`

This function lets you create subscriptions listening to specific logs of things happening on the blockchain, filtered by a given list of topics.

It takes as arguments:
- the [provider](#websocket-provider) map
- map of options:
  - `:from-block` : the block number (greater than or equal to) from which to get events on.
  - `:address` : a string or a vector of strings with addresses to listen to
  - `:topics` : An vector of values which must each appear in the log entries. Order needs to match the order in the `:address` vector.
- a nodejs-style callback function (`error` is a first parameters and `response` the second), executed each time the log is seen (optional)

```clojure
(subscribe-logs web3
                {:address [address]
                 :topics [event-signature]
                 :from-block block-number}
                (fn [_ event] (prn event))
```

Similar to the [subscribe-events](#subscribe-events) function, it returns an `EventEmitter` which can be augmented using [on](#on).

#### <a name= "decode-log">`decode-log`

Use this function to decodes an ABI encoded log data and indexed topic data, such as returned by the [subscribe-logs](#subscribe-logs) subscription.

Arguments:
- the [provider](#websocket-provider) map
`abi` : a map of interface inputs array
`data` : the abi bytecode of the data field in the log (a string)
`topics` : a vector of the topics of the log (see [subscribe-logs](#subscribe-logs))

#### <a name= "on">`on`

This function can be used with the `EventEmitter` returned by the [subscribe-events](#subscribe-events) and [subscribe-logs](#subscribe-logs).
It will add callbacks executed on specific events:
- `:connected`: fires the callback when the subscription is created, returns the id of that subscription as the first argument of the callback
- `:data` : fired on each incoming log with the log object as argument. If the subscription happens ot be listening to a smart event contract the event passed as th ecallback argument is the same as for the [subscribe-events](#subscribe-events).
- `:changed` : fires when the log is removed from the blockchain
- `:error` : fires when an error in the subscription occurs.

```clojure
(-> event-emitter
    (#(on web3 % :connected (fn [sub-id]
                              (prn "subscribed to SetCounterEvents. Subscription id :" sub-id))))
    (#(on web3 % :data (fn [event]
                         (prn "new SetCounterEvents :" event))))
    (#(on web3 % :error (fn [error]
                          (prn "Error :" error)))))
```

#### <a name= "unsubscribe">`unsubscribe`

Clears a subscription, takes a web3 provider an an event emitter (returned by the [subscribe-events](#subscribe-events) or [subscribe-logs](#subscribe-logs)) as arguments.

```clojure
(web3-eth/unsubscribe web3 event-emitter)
 ```

#### <a name= "clear-subscriptions">`clear-subscriptions`

Clears all created subscription.

#### <a name= "get-past-events">`get-past-events`

Returns all past events for the specified contract.

It takes as arguments:
- the [provider](#websocket-provider) map
- smart contract instance, as returned by the [contract-at](#contract-at) function
- the CamelCased keyword with the name of the smart contracts event to replay
- map of options:
  - `:from-block` : the block number (greater than or equal to) from which to return the events
  - `:to-block` : the block number (less than or equal to) up to which the events are returned
- a nodejs-style callback function (`error` is a first parameters and `response` the second), executed each time the event is seen (optional)

```clojure
(web3-eth/get-past-events web3
                          my-contract
                          :SetCounterEvent
                          {:from-block 0
                           :to-block "latest"}
                          (fn [events]))
```

#### <a name= "get-past-logs">`get-past-logs`

A [subscribe-logs](#subscribe-logs) equivalent of [get-past-events](#get-past-events).

```clojure
(web3-eth/get-past-logs web3
                        {:address [address]
                         :topics [event-signature]
                         :from-block 0
                         :to-block "latest"}
                        (fn [logs]))
```

### <a name="utils">`cljs-web3.utils`

This namespaces provides various utility functions.

#### <a name="sha3">`sha3`

Returns a sha3 of the input.

#### <a name="solidity-sha3">`solidity-sha3`

Implementation of Solidity sha3 function. Takes a web3 provider and a variable number of arguments, returns a hash value (a string).

```
(solidity-sha3 web3 "0x7d10b16dd1f9e0df45976d402879fb496c114936" 6 "abc")
```

#### <a name="from-ascii">`from-ascii`

`(from-ascii web3 args)`

#### <a name="to-ascii">`to-ascii`

`(to-ascii web3 arg)`

#### <a name="number-to-hex">`number-to-hex`

`(number-to-hex web3 number)`

#### <a name="from-wei">`from-wei`

`(from-wei web3 number <unit>)`

#### <a name="to-wei">`to-wei`

`(to-wei web3 number <unit>)`

#### <a name="address?">`address?`

`(address? web3 address)`

### <a name="evm">`cljs-web3.evm`

*NOTE* The functions in this namespaces are not a part of the API unless you [extend](#extend) the `evm` module with these RPC calls.
They will only to work with a testrpc such as [ganache](https://www.trufflesuite.com/ganache)

#### <a name="increase-time">`increase-time`

Increases the blockchain time by the specified numebr of seconds.

`(increase-time web3 seconds)`

#### <a name="mine-block">`mine-block`

Instantly mines a block.

`(mine-block web3)`

### <a name="helpers">`cljs-web3.helpers`

Functions in this namespace are not part of the API, rather help in turning the JS objects returned by the API funcions to the corresponding Clojure data structures.
As such they are independent of the currently used implementation and do not take the web3 provider as their first argument.

#### <a name="js->cljkk">`js->cljkk`

From JavaScript Object to Clojure map with kebab-cased keywords, e.g. :

```clojure
#js {:fromBlock 0, :toBlock "latest"}
;; =>
{:from-block 0 :to-block "latest"}
```
#### <a name="cljkk->js">`cljkk->js`

From Clojure with kebab-cased keywords to JavaScript e.g.

```clojure
{:from-block 0 :to-block "latest"}
;; =>
#js {:fromBlock 0, :toBlock "latest"}
```

#### <a name="event-interface">`event-interface`

Given a contract instance returned by the [contract-at](#contract-at) function and a `:CamelCase` key of the event, returns the [abi](#abi) interface of that event, which can be used e.g. with [subscribe-logs](#subscribe-logs).

```clojure
(event-interface my-contract :SetCounterEvent)
```

#### <a name="return-values->clj">`return-values->clj`

Given a `returnValues` field (part of the data return by subscriptions, such as [subscribe-events](#subscribe-events)) and an [event-interface](#event-interface) returns a edn (aka Clojure) representation of this events return values.

```
(return-values->clj return-values event-interface)
```
