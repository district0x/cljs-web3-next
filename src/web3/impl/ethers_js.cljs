(ns web3.impl.ethers-js
  (:require [web3.core :as web3-core]))

(defrecord EthersJS [provider])

(defn make-ethers-js [provider]
  (->EthersJS provider))

(defn- build-event [ev]
  {:web3.block/number (-> ev .-blockNumber)
   :web3.event/data (let [oks (js/Object.keys (.-args ev))]
                      (reduce (fn [r k]
                                (assoc r (keyword k) (aget (.-args ev) k)))
                              {} oks))
   :web3.event/name (-> ev .-event)
   :web3.event/signature (-> ev .-eventSignature)
   :event-type :tx-log})

(extend-type EthersJS

  web3-core/Events

  (-past-events [{:keys [provider] :as this}
                 contract-instance
                 {:keys [from-block] :as opts}
                 {:keys [on-result on-progress on-error]}]
    (let [contract (new js/ethers.Contract
                        (web3-core/contract-address contract-instance)
                        (web3-core/contract-abi contract-instance) provider)
          all-evs (atom [])
          finished? (atom false)]
      (web3-core/last-block-number this
                                   {:on-result
                                    (fn [lb]
                                      (println "Downloading events, from block " from-block " to " lb)
                                      (.resetEventsBlock provider from-block)
                                      ;; asumes blocks come in order
                                      (.on contract "*"
                                           (fn [ev]
                                             (if (< (.-blockNumber ev) lb)
                                               (swap! all-evs conj (build-event ev))

                                               ;; TODO: missing all the events from last block
                                               ;; the problem is when to call the callback
                                               (when-not @finished?
                                                 (reset! finished? true)
                                                 (println "Events downloaded" @finished?)
                                                 (.removeAllListeners contract "*")
                                                 (swap! all-evs conj (build-event ev))
                                                 (on-result @all-evs))))))})))

  (-on-new-event [{:keys [provider]} contract-instance _ {:keys [on-result on-error]}]
    (let [ contract (new js/ethers.Contract
                        (web3-core/contract-address contract-instance)
                        (web3-core/contract-abi contract-instance) provider)]

      (.on contract "*"
           (fn [ev]
             (on-result (build-event ev))))))


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

  (-call-constant [{:keys [provider contracts-map]} contract-key method args opts callbacks]
    )
  (-call-tx [{:keys [provider contracts-map]} contract-key method args opts callbacks]
    ))
