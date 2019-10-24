# cljs-web3-next

[![Build Status](https://travis-ci.org/district0x/cljs-web3-next.svg?branch=master)](https://travis-ci.org/district0x/cljs-web3-next)

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

### <a name="core"></a>`cljs-web3.core`
#### <a name="http-provider">`http-provider`
#### <a name="connection-url" >`connection-url`
#### <a name="websocket-provider" >`websocket-provider`
#### <a name="extend">`extend`
#### <a name="connected?">`connected?`
#### <a name="disconnect">`disconnect`
#### <a name="on-connect">`on-connect`
#### <a name="on-disconnect">`on-disconnect`
#### <a name="on-error">`on-error`

### <a name="eth" >`cljs-web3.eth`
#### <a name= "is-listening?">`is-listening?`
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
