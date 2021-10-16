(ns cljs-web3-next.settings
  (:require [oops.core :refer [ocall oget oset! oapply+]]))

(defn settings [web3]
  (oget web3 "settings"))

(defn default-account [web3]
  (oget web3 "settings" "defaultAccount"))

(defn set-default-account! [web3 hex-str]
  (oset! (settings web3) "defaultAccount" hex-str))

(defn default-block [web3]
  (oget web3 "settings" "defaultBlock"))

(defn set-default-block! [web3 block]
  (oset! (settings web3) "defaultBlock" block))
