(ns web3.impl.ethers-js
  (:require [web3.core :as web3-core]
            cljsjs.bignumber
            [goog.string :as gstring]))

(defrecord EthersJS [signer provider])

(defn ethers-js-bignumber? [x]
  (= (aget x "_ethersType") "BigNumber"))

(defn normalize-object [n]
  (cond
    (ethers-js-bignumber? n) (js/BigNumber. (.toString n))
    :else n))

(defn make-ethers-js [signer provider]
  (->EthersJS signer provider))

(defn- build-event [ev]
  {:web3.block/number (normalize-object (-> ev .-blockNumber))
   :web3.event/data (let [oks (js/Object.keys (.-args ev))]
                      (reduce (fn [r k]
                                (assoc r
                                       (keyword k)
                                       (normalize-object (aget (.-args ev) k))))
                              {} oks))
   :web3.event/name (-> ev .-event)
   :web3.event/signature (-> ev .-eventSignature)
   :event-type :tx-log})

(extend-type EthersJS

  web3-core/Events

  (-on-event [{:keys [provider]} contract-instance {:keys [from-block]} {:keys [on-event-result on-error]}]
    (let [ contract (new js/ethers.Contract
                        (web3-core/contract-address contract-instance)
                        (web3-core/contract-abi contract-instance) provider)]
      (when from-block
        (println "Reseting block to " from-block)
        (.resetEventsBlock provider from-block))
      (.on contract "*"
           (fn [ev]
             (on-event-result (build-event ev))))))

  (-on-new-block [_ callback]
    ;; TODO: Implement this
    )


  web3-core/Blockchain

  (-last-block-number [{:keys [provider]} {:keys [on-result on-error]}]
    (.then (.getBlockNumber provider) #(on-result %)))

  (-block [{:keys [provider contracts-map]} number {:keys [on-result on-error]}]
    )
  (-tx [{:keys [provider contracts-map]} tx-hash {:keys [on-result on-error]}]
    )
  (-tx-receipt [{:keys [provider contracts-map]} tx-hash {:keys [on-result on-error]}]
    )


  web3-core/Account

  (-balance [{:keys [provider contracts-map]} id {:keys [on-result on-error]}]
    )


  web3-core/NameService

  (-resolve-name [{:keys [provider contracts-map]} name {:keys [on-result on-error]}]
    )
  (-lookup-address [{:keys [provider contracts-map]} address {:keys [on-result on-error]}]
    )


  web3-core/ContractExecution

  (-contract-call [{:keys [signer provider] :as web3} contract-instance method args opts {:keys [on-result
                                                                                                 on-tx-receipt
                                                                                                 on-error]}]
    (println (gstring/format "Calling contract (%s)" (web3-core/contract-address contract-instance)))
    (let [contract (new js/ethers.Contract
                        (web3-core/contract-address contract-instance)
                        (web3-core/contract-abi contract-instance)
                        signer)
          contract-method (aget contract (name method))]
      (.then (apply contract-method args)
             (fn [tx]
               (println "Got tx hash " (.-hash tx))
               (on-result {:tx-hash (.-hash tx)})
               (if-let [wait-fn (.-wait tx)]
                 (.then (wait-fn 1)
                        (fn [receipt]
                          (.log js/console "Got receipt " receipt)
                          (on-tx-receipt {:events (.-events receipt)
                                          :status (.-status receipt)})))
                 (println "No wait fn in tx, assuming constant call")))))))
