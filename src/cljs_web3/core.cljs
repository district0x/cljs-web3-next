(ns cljs-web3.core
  (:require [cljs.nodejs :as nodejs]
            [cljs-web3.helpers :as web3-helpers]))

(def Web3 (nodejs/require "web3"))

(defn http-provider [uri]
  {:provider (new Web3 (new (aget Web3 "providers" "HttpProvider") uri))})

(defn websocket-provider [uri]
  {:provider (new Web3 (new (aget Web3 "providers" "WebsocketProvider") uri))})

(defn connection-url [{:keys [:provider]}]
  (aget provider "currentProvider" "connection" "_url"))

(defn current-provider [{:keys [:provider]}]
  (aget provider "currentProvider"))

(defn set-provider [{:keys [:provider]} new-provider]
  (js-invoke provider "setProvider" new-provider))

(defn extend [{:keys [:provider]} property methods]
  {:provider (js-invoke provider "extend" (web3-helpers/cljkk->js {:property property
                                                                   :methods methods}))})

(defn connected? [{:keys [:provider]}]
  (aget provider "currentProvider" "connected"))

(defn disconnect [{:keys [:provider]}]
  (js-invoke (aget provider "currentProvider") "disconnect"))

(defn on-connect [{:keys [:provider]} & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "connect" callback])))

(defn on-disconnect [{:keys [:provider]} & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "end" callback])))

(defn on-error [{:keys [:provider]} & [callback]]
  (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "error" callback])))
