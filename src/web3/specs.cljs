(ns web3.specs
  (:require [clojure.spec.alpha :as s]))


(s/def :web3/obj any?)
(s/def :web3.block/number number?)

;; TODO shape this
(s/def :web3/block any?)
(s/def :web3.tx/hash string?)
(s/def :web3.opt/from-block :web3.block/number)

;; This event-type is only for allowing other kinds of events in
;; the future, currently we call events only to tx logs
(s/def :web3.opt/event-type #{:tx-log})

(s/def :web3.event/types any?)

(s/def :web3.event/data (s/map-of keyword? :web3.event/types))
(s/def :web3.event/name string?)
(s/def :web3.event/signature string?)

(s/def :web3/event (s/keys :req [:web3.block/number
                                 :web3.event/data
                                 :web3.event/name
                                 :web3.event/signature]
                           :req-un [:web3.opt/event-type]))

(s/def :web3/address string?)
(s/def :web3/abi any?)
(s/def :web3/binary any?)
(s/def :web3.contract/instance (s/keys :req-un [:web3/address
                                                :web3/abi]
                                       :opt-un [:web3/binary]))
(s/def :web3.filter/id keyword?)
(s/def :web3.callback/on-result fn?)
(s/def :web3.callback/on-error fn?)
(s/def :web3.callback/on-progress fn?)

(s/def :web3.callback/on-events-result (s/fspec :args (s/cat :events (s/coll-of :web3/event))))
(s/def :web3.callback/on-event-result (s/fspec :args (s/cat :event :web3/event)))
(s/def :web3.callback/on-tx-receipt (s/fspec :args (s/cat :result any?)))
