(ns cljs-web3.utils)

(defn sha3 [provider arg]
  (js-invoke (aget provider "utils") "sha3" arg))

(defn solidity-sha3 [provider arg & args]
  (apply js-invoke (aget provider "utils") "soliditySha3" arg args))

(defn from-ascii [provider arg]
  (js-invoke (aget provider "utils") "fromAscii" arg))

(defn to-ascii [provider arg]
  (js-invoke (aget provider "utils") "toAscii" arg))

(defn number-to-hex [provider number]
  (js-invoke (aget provider "utils") "numberToHex" number))

(defn from-wei [provider number & [unit]]
  (js-invoke (aget provider "utils") "fromWei" (str number) (name unit)))

(defn to-wei [provider number & [unit]]
  (js-invoke (aget provider "utils") "toWei" (str number) (name unit)))

(defn address? [provider address]
  (js-invoke (aget provider "utils") "isAddress" address))
