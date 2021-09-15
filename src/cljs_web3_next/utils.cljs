(ns cljs-web3-next.utils
  (:require [oops.core :refer [ocall oget]]))

(defn sha3 [provider arg]
  (ocall (oget provider "utils") "sha3" arg))

(defn solidity-sha3 [provider arg & args]
  (ocall (oget provider "utils") "soliditySha3" arg args))

(defn from-ascii [provider arg]
  (ocall (oget provider "utils") "fromAscii" arg))

(defn to-ascii [provider arg]
  (ocall (oget provider "utils") "toAscii" arg))

(defn to-hex
  "Will auto convert any given value to HEX.
  Number strings will interpreted as numbers.
  Text strings will be interpreted as UTF-8 strings."
  [provider arg]
  (ocall (oget provider "utils") "toHex" arg))

(defn number-to-hex [provider number]
  (ocall (oget provider "utils") "numberToHex" number))

(defn from-wei [provider number & [unit]]
  (ocall (oget provider "utils") "fromWei" (str number) (name unit)))

(defn to-wei [provider number & [unit]]
  (ocall (oget provider "utils") "toWei" (str number) (name unit)))

(defn address? [provider address]
  (ocall (oget provider "utils") "isAddress" address))

(defn address->checksum [provider address]
  (ocall (oget provider "utils") "toChecksumAddress" address))
