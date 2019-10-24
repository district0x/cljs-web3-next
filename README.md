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
