(ns cljs-web3-next.core
  (:require [cljs.nodejs :as nodejs]
            [oops.core :refer [ocall ocall+ oget gget]]
            [cljs-web3-next.utils :as utils]
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

;; compatible API polyfills (proxies)

(defn default-web3 []
  (new Web3 (gget "web3" "currentProvider" )))


(def version-ethereum
  "Returns a hexadecimal string representing the Ethereum protocol version.
  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result
  Example:
  user> `(version-ethereum web3-instance
           (fn [err res] (when-not err (println res))))`
  nil
  user> 0x3f"
  (oget (default-web3) "version"))


(defn sha3
  "Returns a string representing the Keccak-256 SHA3 of the given data.

  Parameters:
  String - The string to hash using the Keccak-256 SHA3 algorithm
  Map    - (optional) Set encoding to hex if the string to hash is encoded
                      in hex. A leading 0x will be automatically ignored.
  Web3   - (optional first argument) Web3 JavaScript object.

  Example:
  user> (def hash \"Some string to be hashed\")
  #'user/hash
  user> `(sha3 hash)
  \"0xed973b234cf2238052c9ac87072c71bcf33abc1bbd721018e0cca448ef79b379\"`
  user> `(sha3 hash {:encoding :hex})`
  \"0xbd83a94d23235dd7dfcf67a5a0d9e9643a715cd5b528083a2cf944d61f8e7b51\"

  NOTE: This differs from the documented result of the Web3 JavaScript API,
  which equals
  \"0x85dd39c91a64167ba20732b228251e67caed1462d4bcf036af88dc6856d0fdcc\""
  ([string] (sha3 string nil))
  ([string options] (utils/sha3 (default-web3) string options))
  ([Web3 string options]
   (ocall+ Web3 "sha3" [string options])))
