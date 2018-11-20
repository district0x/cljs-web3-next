(ns web3.utils)

(defprotocol Util
  (-keccak256 [_ x]))
