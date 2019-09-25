(ns district.server.dev
  (:require [district.server.config :refer [config]]
            [district.server.logging :refer [logging]]
            [district.server.web3 :refer [web3]]
            [district.server.syncer]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3-events]
            [district.server.constants :as constants]
            [cljs.nodejs :as nodejs]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [district.shared.smart-contracts :as smart-contracts-dev]
            [district.shared.async-helpers :as async-helpers :refer [promise->]]
            [web3.impl.web3js :as web3js]
            [cljs-web3.core :as web3-core]
            [cljs-web3.eth :as web3-eth]
            ))

(nodejs/enable-util-print!)

(def contracts-var
  #'smart-contracts-dev/smart-contracts)

(defn start []
  (-> (mount/with-args
        {:config {:default {:logging {:level :info
                                      :console? true}
                            :web3 {:url "http://localhost:8549"}
                            :smart-contracts {:contracts-var contracts-var}
                            :web3-events {:events constants/web3-events}
                            }}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config @config}))))

(defn stop []
  (mount/stop))

(defn on-jsload []
  (log/debug "on-jsload")
  (stop)
  (start))

(defn -main [& args]

  (async-helpers/extend-promises-as-channels!)

  (log/debug "Starting...")
  (start))

(set! *main-cli-fn* -main)

;; IN-PROGRESS : syncer for MyContract
;; TODO : generator for MyContract
(defn test-it []
  (let [events {:my-contract/set-counter-event [:my-contract :SetCounterEvent]}
        new-sub (atom nil)
        tx-hash "0xa5b7c7851f649be2084a7be694f40bda9756b72f4d2139b75a0122bbd337281e"
        inst (web3js/new)
        web3-inst {:instance inst
                   :provider (web3-core/websocket-provider inst "ws://127.0.0.1:8549")}]
    (promise-> (web3-eth/is-listening? web3-inst)

               #(prn "connected?:" %)

               #(web3-eth/accounts web3-inst)
               #(prn "accounts:" %)

               #(web3-eth/get-block-number web3-inst)
               (fn [block-number]
                 (web3-eth/get-block web3-inst block-number false (fn [err res]
                                                                    (let [{:keys [:timestamp]} (js->clj res :keywordize-keys true)]
                                                                      (log/debug "timestamp" {:timestamp timestamp})
                                                                      (js/Promise.resolve timestamp)))))
               #(prn "block :" %)

               #(web3-eth/get-transaction-receipt web3-inst tx-hash)
               #(prn "past tx-receipt:" %)

               ;; replay past events
               #_#(smart-contracts/replay-past-events-in-order events (fn [error event]
                                                                        (log/debug "replaying past event" event))
                                                               {:from-block 0
                                                                :to-block "latest"
                                                                :on-finish (fn []
                                                                             (log/debug "Finished replaying past events"))})

               ;; watch for new events
               #(web3-eth/get-block-number web3-inst)
               (fn [last-block-number]
                 (let [event-emitter (smart-contracts/subscribe-events :my-contract
                                                                       :SetCounterEvent
                                                                       {:from-block last-block-number}
                                                                       (fn [error tx]
                                                                         (log/debug "new event subscribe-events/callback" {:tx tx})))]

                   #_(web3-eth/on web3-inst event-emitter :data (fn [tx]
                                                                (log/debug "new event subscribe-events/on-data" {:tx tx})))

                   (reset! new-sub event-emitter)))
               #_(fn [last-block-number]
                   (let [event-emitter (smart-contracts/subscribe-logs :my-contract
                                                                       :SetCounterEvent
                                                                       {:from-block last-block-number}
                                                                       (fn [error tx]
                                                                         (log/debug "new event subscribe-logs/callback" {:tx tx})))]

                   #_(web3-eth/on web3-inst event-emitter :data (fn [tx]
                                                                (log/debug "new event subscribe-logs/on-data" {:tx tx})))

                   (reset! new-sub event-emitter)))

               ;; #(smart-contracts/contract-call :my-contract :counter)
               ;; #(prn "result:" %)

               #(smart-contracts/contract-send :my-contract :set-counter [3] {:gas 5000000})
               ;; ;; #(smart-contracts/contract-send :my-contract :increment-counter [1] {:gas 5000000})
               ;; ;; #(log/debug "send-tx-receipt" {:receipt %})

               #(web3-eth/unsubscribe web3-inst @new-sub (fn [err succ]
                                                           (if succ
                                                             (log/debug "Succesfully unsubscribed" {:return succ})
                                                             (log/debug "Error unsubscribing" {:error err}))))

               )))
