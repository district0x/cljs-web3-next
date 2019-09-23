(ns district.server.syncer
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.core.async :as async]

            ;; [bignumber.core :as bn]

            [camel-snake-kebab.core :as camel-snake-kebab]
            [district.shared.async-helpers :refer [safe-go <?]]
            [district.server.config :refer [config]]
            [district.server.web3 :refer [web3]]
            [cljs-web3.eth :as web3-eth]
            [district.server.web3-events :as web3-events]))

(defn on-set-counter-event [_ {:keys [:args] :as event}]
  (log/debug "on-set-counter-event" args)
  (js/Promise.resolve :on-set-counter-event))

(defn on-increment-counter-event [_ {:keys [:args] :as event}]
  (log/debug "on-increment-counter-event" args)
  (js/Promise.resolve :on-increment-counter-event))

(defn- block-timestamp* [block-number]
  (let [out-ch (async/chan)]
    (web3-eth/get-block @web3 block-number false (fn [err res]
                                                   (let [{:keys [:timestamp] :as result} (js->clj res :keywordize-keys true)]
                                                     (if err
                                                       (js/Error. err)
                                                       (do
                                                         (log/debug "cache miss for block-timestamp" {:block-number block-number :timestamp timestamp})
                                                         (async/put! out-ch timestamp))))))
    out-ch))

(def block-timestamp
  (memoize block-timestamp*))

(defn- get-event [{:keys [:event :log-index :block-number]
                   {:keys [:contract-key :forwards-to]} :contract}]
  {:event/contract-key (name (or forwards-to contract-key))
   :event/event-name (name event)
   :event/log-index log-index
   :event/block-number block-number})

;; TODO : all calls
(defn- dispatcher [callback]
  (fn [err {:keys [:block-number] :as event}]
    (safe-go
     (try

       ;; (<? (callback err event))

       (let [block-timestamp (<? (block-timestamp block-number))
             event (-> event
                       (update :event camel-snake-kebab/->kebab-case)
                       (assoc :timestamp block-timestamp )
                       #_(update-in [:args :timestamp] (fn [timestamp]
                                                       (if timestamp
                                                         (bn/number timestamp)
                                                         block-timestamp)))
                       #_(update-in [:args :version] bn/number))

             {:keys [:event/contract-key :event/event-name :event/block-number :event/log-index] :as a} (get-event event)
             #_{:keys [:event/last-block-number :event/last-log-index :event/count]
              :or {last-block-number -1
                   last-log-index -1
                   count 0}} #_(db/get-last-event {:event/contract-key contract-key :event/event-name event-name} [:event/last-log-index :event/last-block-number :event/count])
             #_evt #_{:event/contract-key contract-key
                  :event/event-name event-name
                  :event/count count
                  :last-block-number last-block-number
                  :last-log-index last-log-index
                  :block-number block-number
                  :log-index log-index}

             ]

         (log/debug "@@@ syncer/dispatcher" a)

         #_(if (or (> block-number last-block-number)
                 (and (= block-number last-block-number) (> log-index last-log-index)))
           (do
             (log/info "Handling new event" evt)
             (let [res (callback err event)] ;; block if we need
               (db/upsert-event! {:event/last-log-index log-index
                                  :event/last-block-number block-number
                                  :event/count (inc count)
                                  :event/event-name event-name
                                  :event/contract-key contract-key})
               res))

           (log/info "Skipping handling of a persisted event" evt)))



       (catch js/Error error
         (log/error "Exception when handling event" {:error error
                                                     :event event})
         ;; So we crash as fast as we can and don't drag errors that are harder to debug
         (js/process.exit 1))))))

(defn start [opts]
  (safe-go
   (when-not (<? (web3-eth/is-listening? @web3))
     (throw (js/Error. "Can't connect to Ethereum node"))))
  (let [event-callbacks {:my-contract/set-counter-event on-set-counter-event
                         :my-contract/increment-counter-event on-increment-counter-event}
        callback-ids (doall (for [[event-key callback] event-callbacks]
                              (web3-events/register-callback! event-key (dispatcher callback))))]

    (log/debug "Syncer/start" {:callback-ids callback-ids})

    (web3-events/register-after-past-events-dispatched-callback! #(log/warn "Syncing past events finished" ::start))
    (assoc opts :callback-ids callback-ids)))

(defn stop [syncer]
  (web3-events/unregister-callbacks! (:callback-ids @syncer)))

(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))
