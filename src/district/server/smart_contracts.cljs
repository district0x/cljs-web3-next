(ns district.server.smart-contracts
  (:require

   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.utils :as web3-utils]

   [district.shared.async-helpers :refer [promise->]]
   [taoensso.timbre :as log]

   [cljs.core.async :refer [<! timeout] :as async]
   [cljs.core.match :refer-macros [match]]
   [cljs.nodejs :as nodejs]
   [cljs.pprint]
   [cljs.spec.alpha :as s]
   [clojure.string :as string]
   [district.shared.async-helpers :as asynch]
   [district.server.config :refer [config]]
   [district.server.web3 :refer [web3]]
   [mount.core :as mount :refer [defstate]]
   [cljs.core.async.impl.protocols])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def fs (nodejs/require "fs"))
(def process (nodejs/require "process"))

(declare start)
;; (declare wait-for-tx-receipt)

(defstate smart-contracts :start (start (merge (:smart-contracts @config)
                                               (:smart-contracts (mount/args)))))

(defn contract [contract-key]
  (get @(:contracts @smart-contracts) contract-key))

(defn contract-address [contract-key]
  (:address (contract contract-key)))

(defn contract-name [contract-key]
  (:name (contract contract-key)))

(defn contract-abi [contract-key]
  (:abi (contract contract-key)))

(defn contract-bin [contract-key]
  (:bin (contract contract-key)))

(defn instance
  ([contract-key]
   (let [contr (contract contract-key)]
     (if-not (:forwards-to contr)
       (:instance contr)
       (instance (:forwards-to contr) contract-key))))
  ([contract-key contract-key-or-addr]
   (web3-eth/contract-at @web3 (contract-abi contract-key) (if (keyword? contract-key-or-addr)
                                                             (contract-address contract-key-or-addr)
                                                             contract-key-or-addr))))

(defn contract-by-address [contract-address]
  (reduce
    (fn [_ [contract-key {:keys [:address] :as contract}]]
      (when (= contract-address address)
        (reduced (assoc contract :contract-key contract-key))))
    ;; nil
    @(:contracts @smart-contracts)))

(defn update-contract! [contract-key contract]
  (swap! (:contracts @smart-contracts) update contract-key merge contract))

(defn- fetch-contract
  "Given a file-name and a path tries to load abi and bytecode.
  It first try to load it from a json truffle artifact, if it doesn't find it
  tries .abi .bin files for the name.
  Returns a map with :abi and :bin keys."
  [file-name & [{:keys [:path]}]]
  (let [path (or path (str (.cwd process) "/resources/public/contracts/build/"))
        json-file-path (str path file-name ".json")
        abi-file-path (str path file-name ".abi")
        bin-file-path (str path file-name ".bin")]
    (if (.existsSync fs json-file-path)

      (let [content-str (.readFileSync fs json-file-path "utf-8")
            json-file-content (js/JSON.parse content-str)]

        {:abi (aget json-file-content "abi")
         :bin (aget json-file-content "bytecode")})
      {:abi (when (.existsSync fs abi-file-path) (js/JSON.parse (.readFileSync fs abi-file-path "utf-8")))
       :bin (when (.existsSync fs bin-file-path) (.readFileSync fs bin-file-path "utf-8"))})))

(defn load-contract-files [contract {:keys [:contracts-build-path]}]
  (let [{:keys [abi bin]} (fetch-contract (:name contract) {:path contracts-build-path})]

    (when-not abi
      (println "Couldn't find ABI for " (:name contract)))

    (when-not bin
      (println "Couldn't find bin for " (:name contract)))

    (merge contract
           {:abi abi
            :bin bin
            :instance (web3-eth/contract-at @web3 abi (:address contract))})))

(defn start [{:keys [contracts-build-path :contracts-var print-gas-usage?] :as opts}]

  (merge
   {:contracts (atom (into {} (map (fn [[k v]]
                                     [k (load-contract-files v opts)])
                                   @contracts-var)))}
   opts))

#_(defn link-library [bin placeholder library-address]
  (string/replace bin placeholder (subs library-address 2)))


#_(defn link-contract-libraries [smart-contracts bin library-placeholders]
  (reduce (fn [bin [placeholder replacement]]
            (let [address (if (keyword? replacement)
                            (get-in smart-contracts [replacement :address])
                            replacement)]
              (link-library bin placeholder address)))
          bin library-placeholders))

#_(defn- handle-deployed-contract! [contract-key {:keys [:abi :bin] :as contract} tx-hash]
  (let [{:keys [:gas-used :block-number :contract-address]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        contract (merge contract {:instance (web3-eth/contract-at @web3 abi contract-address)
                                  :address contract-address})]
    (when (and gas-used block-number)
      (update-contract! contract-key contract)
      (when (:print-gas-usage? @smart-contracts)
        (println (:name contract) contract-address (.toLocaleString gas-used)))
      contract)))

#_(defn deploy-smart-contract!
  "# arguments:
     * `contract-key` keyword e.g. :some-contract
   ## `args` is a vector of arguments for the constructor
   ## `opts` is a map of options:
    * `placeholder-replacements` : a map containing replacements for library placeholders
    * `from` : address deploying the conract
    * `:gas` : gas limit for the contract creation transaction
   # returns:
   function returns a Promise resolving to the deployed contracts address."
  ([contract-key args {:keys [:placeholder-replacements :from :gas] :as opts}]
   (let [{:keys [:abi :bin] :as contract} (load-contract-files (contract contract-key) @smart-contracts)
         opts (merge {:data (str "0x" (link-contract-libraries @(:contracts @smart-contracts) bin placeholder-replacements))}
                     (when-not from
                       {:from (first (web3-eth/accounts @web3))})
                     (when-not gas
                       {:gas 4000000})
                     opts)]
     (-> (js/Promise.resolve
          (-> (apply web3-eth/contract-new @web3 abi
                     (merge args (select-keys opts [:from :to :gas-price :gas
                                                    :value :data :nonce
                                                    :condition])))
              (aget "transactionHash")))
         (.then #(wait-for-tx-receipt %))
         (.then (fn [receipt]
                  (handle-deployed-contract! contract-key contract (:transaction-hash receipt)))))))

  ([contract-key args]
   (deploy-smart-contract! contract-key args {:from (first (web3-eth/accounts @web3))
                                              :gas 4000000})))

#_(defn write-smart-contracts! []
  (let [{:keys [:ns :file :name]} (meta (:contracts-var @smart-contracts))]
    (.writeFileSync fs file
                    (str "(ns " ns ") \n\n"
                         "(def " name " \n"
                         (as-> @(:contracts @smart-contracts) $
                           (map (fn [[k v]]
                                  [k (dissoc v :instance :abi :bin)]) $)
                           (into {} $)
                           ;; cljs.pprint/write won't compile with simple optimisations
                           ;; therefore must be required only in dev environment
                           (cljs.pprint/write $ :stream nil))
                         ")"))))

(defn instance-from-arg [contract & [{:keys [:ignore-forward?]}]]
  (cond
    (and ignore-forward? (keyword? contract)) (instance contract contract)
    (keyword? contract) (instance contract)
    (sequential? contract) (instance (first contract) (second contract))
    :else contract))

#_(defn- wait-for-tx-receipt*
  "callback is a nodejs style callback i.e. (fn [error data] ...)"
  [tx-hash callback]
  (web3-eth/get-transaction-receipt @web3 tx-hash (fn [error receipt]
                                                    (if error
                                                      (callback error nil)
                                                      (if receipt
                                                        (callback nil receipt)
                                                        (js/setTimeout #(wait-for-tx-receipt* tx-hash callback) 1000))))))

#_(defn wait-for-tx-receipt
  "blocks until transaction `tx-hash` gets sent to the network. returns js/Promise"
  [tx-hash]
  (js/Promise. (fn [resolve reject]
                 (wait-for-tx-receipt* tx-hash (fn [error tx-receipt]
                                                 (if error
                                                   (reject error)
                                                   (resolve tx-receipt)))))))

#_(defn wait-for-tx-receipt
  "blocks until transaction `tx-hash` gets sent to the network. returns js/Promise"
  [tx-hash]
  (promise-> (web3-eth/get-transaction-receipt @web3 tx-hash)
             (fn [receipt]
               (if receipt
                 receipt
                 (js/setTimeout #(wait-for-tx-receipt tx-hash) 1000)))))

(defn contract-call
  "Will call a method and execute its smart contract method in the EVM without sending any transaction.
   # arguments:
   ## `contract` parameter can be one of:
   * keyword :some-contract
   * tuple of keyword and address [:some-contract 0x1234...]
   * instance SomeContract
   ## `method` is a :camel_case keyword corresponding to the smart-contract function
   ## `args` is a vector of arguments for the `method`
   ## `opts` is a map of options passed as message data
   # returns:
   function returns a Promise resolving to the result of `method` call."
  ([contract method args {:keys [:ignore-forward?] :as opts}]
   (web3-eth/contract-call @web3
                           (instance-from-arg contract {:ignore-forward? ignore-forward?})
                           method
                           args
                           (dissoc opts :ignore-forward?)))
  ([contract method args]
   (contract-call contract method args {}))
  ([contract method]
   (contract-call contract method [] {})))

(defn contract-send
  "Will send a transaction to the smart contract and execute its method.
   # arguments:
   ## `contract` parameter can be one of:
   * keyword :some-contract
   * tuple of keyword and address [:some-contract 0x1234...]
   * instance SomeContract
   ## `method` is a :camel_case keyword corresponding to the smart-contract function
   ## `args` is a vector of arguments for the `method`
   ## `opts` is a map of options passed as message data
   # returns:
   function returns a Promise resolving to a tx receipt."
  ([contract method args {:keys [:from :gas :ignore-forward?] :as opts}]
   (promise-> (if from
                (js/Promise.resolve from)
                (web3-eth/accounts @web3))
              (fn [accounts]
                (let [opts (merge (when-not from
                                    {:from (first accounts)})
                                  (when-not gas
                                    {:gas 4000000})
                                  (dissoc opts :ignore-forward?))]
                  (web3-eth/contract-send @web3
                                          (instance-from-arg contract {:ignore-forward? ignore-forward?})
                                          method
                                          args
                                          opts)))))
  ([contract method args]
   (contract-send contract method args {}))
  ([contract method]
   (contract-send contract method [] {})))

(defn subscribe-logs [contract event {:keys [:from-block :address :topics :ignore-forward?] :as opts} & [callback]]
  (let [contract-instance (instance-from-arg contract {:ignore-forward? ignore-forward?})
        event-signature (:signature (web3-utils/event-interface contract-instance event))]
    (web3-eth/subscribe-logs @web3
                             contract-instance
                             (merge {:address (aget contract-instance "options" "address")
                                     :topics [event-signature]}
                                    opts)
                             callback)))

#_(defn create-event-filter
  "This function installs event filter
   # arguments:
   ## `contract` parameter can be one of:
    * keyword :some-contract
    * tuple of keyword and address [:some-contract 0x1234...]
    * instance SomeContract
   ## `event` : camel_case keyword corresponding to the smart-contract event
   ## `filter-opts` : a map of indexed return values you want to filter the logs by e.g. {:valueA 1 :valueB 2}
   ## `opts` : specifies additional filter options, can be one of:
    * string 'latest' to specify that only new observed events should be processed
    * map {:from-block 0 :to-block 100} specifying earliest and latest block on which the event handler should fire
   ## `on-event` : event handler function
   see https://github.com/ethereum/wiki/wiki/JavaScript-API#contract-events for additional details"
  [contract event filter-opts opts & [on-event {:keys [:ignore-forward?]}]]
  (apply web3-eth/contract-call (instance-from-arg contract {:ignore-forward? ignore-forward?}) event
         [filter-opts
          opts
          (fn [err log]
            (when on-event
              (if-not log
                (on-event err log)
                (on-event err (enrich-event-log log)))))]))


#_(defn contract-event-in-tx [tx-hash contract event-name & args]
  (let [instance (instance-from-arg contract)
        event-filter (apply web3-eth/contract-call instance event-name args)
        formatter (aget event-filter "formatter")
        contract-addr (aget instance "address")
        {:keys [:logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        signature (aget event-filter "options" "topics" 0)]
    (reduce (fn [result log]
              (when (= signature (first (:topics log)))
                (let [{:keys [:address] :as evt} (js->clj (formatter (clj->js log)) :keywordize-keys true)]
                  (when (= contract-addr address)
                    (reduced (js->cljkk evt))))))
            nil
            logs)))

#_(defn contract-events-in-tx [tx-hash contract event-name & args]
  (let [instance (instance-from-arg contract)
        event-filter (apply web3-eth/contract-call instance event-name args)
        formatter (aget event-filter "formatter")
        contract-addr (aget instance "address")
        {:keys [:logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        signature (aget event-filter "options" "topics" 0)]
    (reduce (fn [result log]
              (when (= signature (first (:topics log)))
                (let [{:keys [:address] :as evt} (js->clj (formatter (clj->js log)) :keywordize-keys true)]
                  (when (= contract-addr address)
                    (concat result [(js->cljkk evt)])))))
            nil
            logs)))

(defn- enrich-event-log [contract-name contract-instance {:keys [:event :return-values] :as log}]
  (-> log
      (update :return-values #(reduce (fn [res value]
                                        (let [n (:name value)]
                                          (assoc res (-> n web3-utils/kebab-case keyword) (aget return-values n))))
                                      {}
                                      (:inputs (web3-utils/event-interface contract-instance event))))
      (update :event (fn [event-name]
                       (if (= (first event-name)
                              (string/upper-case (first event-name)))
                         (keyword event-name)
                         (web3-utils/kebab-case (keyword event-name)))))
      (assoc :contract contract-name)))

(defn replay-past-events-in-order [event-filters callback {:keys [:from-block :to-block
                                                                  :ignore-forward? :delay
                                                                  :transform-fn :on-finish]
                                                           :or {delay 0 transform-fn identity}
                                                           :as opts}]
  (let [logs-chans (for [[k [contract event]] event-filters]
                     (let [logs-ch (async/promise-chan)
                           contract-instance (instance-from-arg contract {:ignore-forward? ignore-forward?})]
                       (web3-eth/get-past-events @web3
                                                 contract-instance
                                                 event
                                                 opts
                                                 (fn [error events]
                                                   (let [logs (->> events
                                                                   web3-utils/js->cljkk
                                                                   (map (partial enrich-event-log contract contract-instance)))]
                                                     (async/put! logs-ch {:err error :logs logs}))))
                       logs-ch))]

    ;; go chan by chan collecting events
    (go-loop [all-logs []
              [logs-ch & rest-logs] logs-chans]
      (if logs-ch
        (let [{:keys [:err :logs]} (async/<! logs-ch)
              logs (map #(assoc % :err err) logs)]
          ;; keep collecting
          (recur (into all-logs logs) rest-logs))

        ;; no more channels to read, sort and callback
        (let [sorted-logs (transform-fn (sort-by (juxt :block-number :transaction-index :log-index) all-logs))]
          (go-loop [logs sorted-logs]
            (if (seq logs)
              (do
                (when (pos? delay)
                  (<! (timeout delay)))
                (let [first-log (first logs)]

                  (when (fn? callback)
                    (doseq [res (callback (:err first-log) (dissoc first-log :err))]

                      ;; if callback returns a promise or chan we block until it resolves
                      (cond
                        (satisfies? cljs.core.async.impl.protocols/ReadPort res) (<! res)
                        (asynch/promise? res)                                    (<! (asynch/promise->chan res))))))
                (recur (rest logs)))

              (when (fn? on-finish)
                (on-finish sorted-logs)))))))))
