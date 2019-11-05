(ns cljs-web3.core
  (:require [cljs.nodejs :as nodejs]
            [cljs-web3.helpers :as web3-helpers]))

(def Web3 (nodejs/require "web3"))

(defn http-provider [uri]
  (new Web3 (new (aget Web3 "providers" "HttpProvider") uri)))

(defn websocket-provider [uri]
  (new Web3 (new (aget Web3 "providers" "WebsocketProvider") uri)))

(defn connection-url [provider]
  (aget provider "currentProvider" "connection" "_url"))

(defn current-provider [provider]
  (aget provider "currentProvider"))

(defn set-provider [provider new-provider]
  (js-invoke provider "setProvider" new-provider))

(defn extend [provider property methods]
  (js-invoke provider "extend" (web3-helpers/cljkk->js {:property property
                                                        :methods methods})))

(defn connected? [provider]
  (aget provider "currentProvider" "connected"))

(defn disconnect [provider]
  (js-invoke (aget provider "currentProvider") "disconnect"))

(defn on-connect [provider & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "connect" callback])))

(defn on-disconnect [provider & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "end" callback])))

(defn on-error [provider & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "error" callback])))
