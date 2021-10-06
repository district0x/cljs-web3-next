(ns cljs-web3-next.bzz
  "Interface to the web3-bzz that allows you to interact with swarm
  decentralized file store"
  (:refer-clojure :exclude [get])
  (:require [cljs-web3-next.helpers :as web3-helpers]
            [oops.core :refer [ocall oget oset! oapply+]]))

(defn get-bzz [provider]
  (oget provider "bzz"))

;;TODO research Swarm docs for shimming
(defn block-network-read [provider & args]
 nil
  #_(oapply+ (get-bzz provider) "blockNetworkRead" args))

(defn download [web3 & args]
  (oapply+ (get-bzz web3) "download" args))

(defn upload [web3 & args]
  (oapply+ (get-bzz web3) "pick" args))

(defn put [web3 & args]
  (oapply+ (get-bzz web3) "upload" args))

;; DEPRECATED
(defn swap-enabled? [web3 & args]
  false)

;; DEPRECATED
(defn sync-enabled? [web3 & args]
  false)

(def get download)
(def retrieve download)
(def modify put)
(def store put)



;; TODO verify both fns
(defn hive [web3]
  (oget (get-bzz web3) "bzz" "hive"))

(defn info [web3]
  (oget (get-bzz web3) "bzz" "info"))
