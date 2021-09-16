(ns cljs-web3-next.core
  (:require [cljs.nodejs :as nodejs]
            [oops.core :refer [ocall ocall+ oget]]
            [cljs-web3-next.helpers :as web3-helpers]))

(def Web3 (nodejs/require "web3"))

(defn http-provider [uri]
  (new Web3 (new (oget Web3 "providers" "HttpProvider") uri)))

(defn websocket-provider [uri opts]
  (new Web3 (new (oget Web3 "providers" "WebsocketProvider") uri (web3-helpers/cljkk->js opts))))

(defn connection-url [provider]
  (oget provider "currentProvider" "connection" "_url"))

(defn current-provider [provider]
  (oget provider "currentProvider"))

(defn set-provider [provider new-provider]
  (ocall+ provider "setProvider" new-provider))

(defn extend [provider property methods]
  (ocall+ provider "extend" (web3-helpers/cljkk->js {:property property
                                                        :methods methods})))

(defn connected? [provider]
  (oget provider "currentProvider" "connected"))

(defn disconnect [provider]
  (ocall (oget provider "currentProvider") "disconnect"))

(defn on-connect [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "connect" callback])))

(defn on-disconnect [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "end" callback])))

(defn on-error [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "error" callback])))
