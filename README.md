# cljs-web3-next

[![CircleCI](https://circleci.com/gh/district0x/cljs-web3-next/tree/master.svg?style=svg&circle-token=d5db014fe5702d820bb4bb42c93959d02fa8ddba)](https://circleci.com/gh/district0x/cljs-web3-next/tree/master)

This ClojureScript library provides a API for interacting with [Ethereum](https://www.ethereum.org/) nodes.
It uses a [bridge pattern](https://en.wikipedia.org/wiki/Bridge_pattern) to decouple its API from the subsequent implementations, allowing the latter to vary at runtime.

In practice what this means is that future versions of the library will maintain the same function signatures and behaviour, it also means that anyone can provide a suitable implementation in this library, or opt-in to use the API in own.

## Installation
Latest released version of this library: <br>
[![Clojars Project](https://img.shields.io/clojars/v/district0x/cljs-web3-next.svg)](https://clojars.org/district0x/cljs-web3-next)

## API Overview

- [cljs-web3.api](#api)
- [cljs-web3.core](#core)
  - [http-provider](#http-provider)
  - [connection-url](#connection-url)
  - [websocket-provider](#websocket-provider)
  - [extend](#extend)
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
Returns a map with two keys:
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

Similar to [websocket-provider](#websocket-provider), but creates a Web3 instance over HTTP.

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



#### <a name= "get-transaction-receipt">`get-transaction-receipt`
#### <a name= "accounts">`accounts`
#### <a name= "get-block-number">`get-block-number`
#### <a name= "get-block">`get-block`
#### <a name= "encode-abi">`encode-abi`
#### <a name= "contract-call">`contract-call`
#### <a name= "contract-send">`contract-send`
#### <a name= "subscribe-events">`subscribe-events`
#### <a name= "subscribe-logs">`subscribe-logs`
#### <a name= "decode-log">`decode-log`
#### <a name= "unsubscribe">`unsubscribe`
#### <a name= "clear-subscriptions">`clear-subscriptions`
#### <a name= "get-past-events">`get-past-events`
#### <a name= "on">`on`

### <a name="utils">`cljs-web3.utils`
#### <a name="sha3">`sha3`
#### <a name="solidity-sha3">`solidity-sha3`
#### <a name="from-ascii">`from-ascii`
#### <a name="to-ascii">`to-ascii`
#### <a name="number-to-hex">`number-to-hex`
#### <a name="from-wei">`from-wei`
#### <a name="to-wei">`to-wei`
#### <a name="address?">`address?`

### <a name="evm">`cljs-web3.evm`
#### <a name="increase-time">`increase-time`
#### <a name="mine-block">`mine-block`

### <a name="helpers">`cljs-web3.helpers`
#### <a name="js->cljkk">`js->cljkk`
#### <a name="cljkk->js">`cljkk->js`
#### <a name="event-interface">`event-interface`
#### <a name="return-values->clj">`return-values->clj`
