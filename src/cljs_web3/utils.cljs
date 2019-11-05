(ns cljs-web3.utils)

(defn sha3 [{:keys [:provider]} args]
  (js-invoke (aget provider "utils") "sha3" arg))

(defn solidity-sha3 [{:keys [:provider]} arg & args]
  (apply js-invoke (aget provider "utils") "soliditySha3" arg args))

(defn from-ascii [{:keys [:provider]} args]
  (js-invoke (aget provider "utils") "fromAscii" arg))

(defn to-ascii [{:keys [:provider]} args]
  (js-invoke (aget provider "utils") "toAscii" arg))

(defn number-to-hex [{:keys [:provider]} number]
  (js-invoke (aget provider "utils") "numberToHex" arg))

(defn from-wei [{:keys [:provider]} number & [unit]]
  (js-invoke (aget provider "utils") "fromWei" (str number) (name unit)))

(defn to-wei [{:keys [:provider]} number & [unit]]
  (js-invoke (aget provider "utils") "toWei" (str number) (name unit)))

(defn address? [{:keys [:provider]} address]
  (js-invoke (aget provider "utils") "isAddress" address))
