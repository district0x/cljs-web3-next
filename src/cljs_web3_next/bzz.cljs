(ns cljs-web3-next.bzz
  "Interface to the web3-bzz that allows you to interact with swarm
  decentralized file store"
  (:refer-clojure :exclude [get])
  (:require [cljs-web3-next.helpers :as web3-helpers]
            [oops.core :refer [ocall oget oset! oapply+]]))

(defn get-bzz [provider]
  (oget provider "bzz"))

(defn block-network-read [provider & args]
  (oapply+ (get-bzz provider) "blockNetworkRead" args))

(defn download [web3 & args]
  (js-apply (get-bzz web3) "download" args))


(def hive (u/prop-or-clb-fn "bzz" "hive"))
(def info (u/prop-or-clb-fn "bzz" "info"))

(defn modify [web3 & args]
  (js-apply (get-bzz web3) "modify" args))

(defn retrieve [web3 & args]
  (js-apply (get-bzz web3) "retrieve" args))

(defn store [web3 & args]
  (oapply+ (get-bzz web3) "store" args))

(defn upload [web3 & args]
  (oapply+ (get-bzz web3) "upload" args))

;; DEPRECATED
(defn swap-enabled? [web3 & args]
  (js-apply (get-bzz web3) "swapEnabled" args))

;; DEPRECATED
(defn sync-enabled? [web3 & args]
  false)

(def get download)
(def put upload)
