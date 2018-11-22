(ns web3.core
  (:require [clojure.spec.alpha :as s]
            [web3.specs]))

;; ABOUT BIG NUMBERS !!!
;; ---------------------

;; All big numbers returned from blockchain are represented using
;; https://github.com/MikeMcl/bignumber.js/

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Web3 protocols ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Events
  (-past-events [_ contract-instance opts callbacks])
  (-on-new-event [_ contract-instance opts callbacks])
  (-on-new-block [_ callback])
  (-stop-listening [_ filter-id]))

(defprotocol Blockchain
  (-last-block-number [_ callbacks])
  (-block [_ number callbacks])
  (-tx [_ tx-hash callbacks])
  (-tx-receipt [_ tx-hash callbacks]))

(defprotocol Account
  (-balance [_ id callbacks]))

(defprotocol NameService
  (-resolve-name [_ name callbacks])
  (-lookup-address [_ address callbacks]))

(defprotocol ContractExecution
  (-contract-call [_ contract-instance method args opts callbacks]))

(defprotocol ContractDeploy
  (-deploy [_ contract-instance callbacks]))

;;;;;;;;;;;;
;; Events ;;
;;;;;;;;;;;;

(s/fdef past-events
        :args (s/cat :web3 :web3/obj
                     :contract-instance :web3.contract/instance
                     :opts (s/keys :req-un [:web3.opt/from-block]
                                   :opt-un [:web3.opt/event-type])
                     :callbacks (s/keys :req-un [:web3.callback/on-events-result]
                                        :opt-un [:web3.callback/on-progress
                                                 :web3.callback/on-error]))
        :ret (s/keys :req-un [:web3.filter/id]))

(defn past-events [web3 contract-instance opts callbacks]
  (let [opts (-> opts
                 (assoc :event-type (or (:event-type opts) :tx-log))
                 (assoc :from-block (or (:from-block opts) 0)))]
    (-past-events web3 contract-instance opts callbacks)))

(s/fdef on-new-event
        :args (s/cat :web3 :web3/obj
                     :contract-instance :web3.contract/instance
                     :opts (s/keys :opt-un [:web3.opt/event-type])
                     :callbacks (s/keys :req-un [:web3.callback/on-event-result]
                                        :opt-un [:web3.callback/on-error]))
        :ret (s/keys :req-un [:web3.filter/id]))

(defn on-new-event [web3 contract-instance opts callbacks]
  (let [opts (-> opts
                 (assoc :event-type (or (:event-type opts) :tx-log)))]
    (-on-new-event web3 contract-instance opts callbacks)))

(s/fdef on-new-block
  :args (s/cat :web3 :web3/obj
               :callback fn? #_(s/fspec :args (s/cat :block :web3/block)))
  :ret (s/keys :req-un [:web3.filter/id]))

(defn on-new-block [web3 callback]
  (-on-new-block web3 callback))

(s/fdef stop-listening
  :args (s/cat :web3 :web3/obj
               :filter-id :web3.filter/id))

(defn stop-listening [web3 filter-id]
  (-stop-listening web3 filter-id))

;;;;;;;;;;;;;;;;
;; Blockchain ;;
;;;;;;;;;;;;;;;;

(s/fdef last-block-number
        :args (s/cat :web3 :web3/obj
                     :callbacks (s/keys :req-un [:web3.callback/on-result]
                                        :opt-un [:web3.callback/on-error]))
        :ret :web3.block/number)

(defn last-block-number [web3 callbacks]
  (-last-block-number web3 callbacks))

(defn block [web3 number callbacks]
  (-block web3 number callbacks))

(defn tx [web3 tx-hash callbacks]
  (-tx web3 tx-hash callbacks))

(defn tx-receipt [web3 tx-hash callbacks]
  (-tx-receipt web3 tx-hash callbacks))

;;;;;;;;;;;;;
;; Account ;;
;;;;;;;;;;;;;

(defn balance [web3 id callbacks]
  (-balance web3 id callbacks))

;;;;;;;;;;;;;;;;;
;; NameService ;;
;;;;;;;;;;;;;;;;;

(defn resolve-name [web3 name callbacks]
  (-resolve-name web3 name callbacks))

(defn lookup-address [web3 address callbacks]
  (-lookup-address web3 address callbacks))

;;;;;;;;;;;;;;;;;;;;;;;
;; ContractExecution ;;
;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef contract-call
  :args (s/cat :web3 :web3/obj
               :contract-instance :web3.contract/instance
               :method keyword?
               :args (s/coll-of any?)
               :opts (s/keys :opt-un []) ;; TODO fill this
               :callbacks (s/keys :opt-un [:web3.callback/on-result
                                           :web3.callback/on-tx-receipt
                                           :web3.callback/on-error])))

(defn contract-call [web3 contract-instance method args opts callbacks]
  (-contract-call web3
                  contract-instance
                  method
                  args
                  opts
                  callbacks))


;;;;;;;;;;;;;;;;;;;;
;; ContractDeploy ;;
;;;;;;;;;;;;;;;;;;;;

(defn deploy [web3 contract-instance callbacks]
  (-deploy web3 contract-instance callbacks))

;;;;;;;;;;;;;;;;;;;
;; Contracts map ;;
;;;;;;;;;;;;;;;;;;;

(s/fdef contract-abi
        :args (s/cat :contract-instance :web3.contract/instance)
        :ret :web3/abi)

(defn contract-abi [contract-instance]
  (:abi contract-instance))

(s/fdef contract-address
        :args (s/cat :contract-instance :web3.contract/instance)
        :ret :web3/address)

(defn contract-address [contract-instance]
  (:address contract-instance))

(defn make-contract-instance [address abi & bin]
  {:address address
   :abi abi})
