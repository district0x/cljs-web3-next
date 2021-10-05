(ns cljs-web3-next.utils
  (:require [camel-snake-kebab.core :as cs :include-macros true]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :refer [>! chan]]
            [oops.core :refer [ocall oget]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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



(defn safe-case [case-f]
  (fn [x]
    (cond-> (subs (name x) 1)
      true (string/replace "_" "*")
      true case-f
      true (string/replace "*" "_")
      true (->> (str (first (name x))))
      (keyword? x) keyword)))

(def camel-case (safe-case cs/->camelCase))
(def kebab-case (safe-case cs/->kebab-case))

(def js->cljk #(js->clj % :keywordize-keys true))

(def js->cljkk
  "From JavaScript to Clojure with kekab-cased keywords."
  (comp (partial transform-keys kebab-case) js->cljk))

(def cljkk->js
  "From Clojure with kebab-cased keywords to JavaScript."
  (comp clj->js (partial transform-keys camel-case)))

(defn callback-js->clj [x]
  (if (fn? x)
    (fn [err res]
      (when (and res (aget res "v"))
        (aset res "v" (aget res "v")))                      ;; Prevent weird bug in advanced optimisations
      (x err (js->cljkk res)))
    x))

(defn args-cljkk->js [args]
  (map (comp cljkk->js callback-js->clj) args))

(defn js-apply
  ([this method-name]
   (js-apply this method-name nil))
  ([this method-name args]
   (let [method-name (camel-case (name method-name))]
     (if (aget this method-name)
       (js->cljkk (apply js-invoke this method-name (args-cljkk->js args)))
       (throw (str "Method: " method-name " was not found in object."))))))

(defn js-prototype-apply [js-obj method-name args]
  (js-apply (aget js-obj "prototype") method-name args))

(defn prop-or-clb-fn
  "Constructor to create an fn to get properties or to get properties and apply a
  callback fn."
  [& ks]
  (fn [web3 & args]
    (if (fn? (first args))
      (js-apply (apply aget web3 (butlast ks))
                (str "get" (cs/->PascalCase (last ks)))
                args)
      (js->cljkk (apply aget web3 ks)))))

(defn create-async-fn [f]
  (fn [& args]
    (let [[ch args] (if (instance? cljs.core.async.impl.channels/ManyToManyChannel (first args))
                      [(first args) (rest args)]
                      [(chan) args])]
      (apply f (concat args [(fn [err res]
                               (go (>! ch [err res])))]))
      ch)))
