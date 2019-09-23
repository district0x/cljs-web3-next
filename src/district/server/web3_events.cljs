(ns district.server.web3-events
  (:require
    [cljs-node-io.core :refer [slurp spit]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [cljs.reader :as reader]
    [clojure.string :as string]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.smart-contracts :as smart-contracts ;;:refer [create-event-filter replay-past-events-in-order]
     ]
    [district.server.web3 :refer [web3]]
    [medley.core :as medley]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))

(defonce fs (nodejs/require "fs"))

(declare start)
(declare stop)

(def default-file-path "web3-events.log")

(defstate ^{:on-reload :noop} web3-events
  :start (start (merge (:web3-events @config)
                       (:web3-events (mount/args))))
  :stop (stop web3-events))


#_(defn register-callback! [event-key callback & [callback-id]]
  (let [[contract-key event] (if-not (= event-key ::after-past-events-dummy-key)
                               (get (:events @web3-events) event-key)
                               [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-id (or callback-id (rand-int 999999999))]

    (when-not contract-key
      (throw (js/Error. "Trying to register callback for non existing event " event-key)))

    (swap! (:callbacks @web3-events) (fn [callbacks]
                                       (-> callbacks
                                         (assoc-in [contract-key event callback-id] callback)
                                         (assoc-in [:callback-id->path callback-id] [contract-key event])))))
  callback-id)


#_(defn register-after-past-events-dispatched-callback! [callback]
  (register-callback! ::after-past-events-dummy-key callback))


#_(defn unregister-callbacks! [callback-ids]
  (doseq [callback-id callback-ids]
    (let [path (get-in @(:callbacks @web3-events) [:callback-id->path callback-id])]
      (swap! (:callbacks @web3-events) (fn [callbacks]
                                         (-> callbacks
                                           (medley/dissoc-in (into path [callback-id]))
                                           (medley/dissoc-in [:callback-id->path callback-id]))))))
  callback-ids)


#_(defn- read-events-from-file [file-path]
  (try
    (reader/read-string {:readers {'object (fn [[t v]]
                                             (if (= "BigNumber" (str t))
                                               (web3/to-big-number v)
                                               nil))}}
                        (str "[" (slurp file-path) "]"))
    (catch js/Error e
      (log/error "Could not read events from file" {:error (ex-message e) :file-path file-path})
      nil)))


#_(defn- append-line-into-file! [file-path line]
  (.appendFileSync fs file-path (str line "\n")))


#_(defn- clear-file! [file-path]
  (.writeFile fs file-path "" (fn [err]
                                (when err
                                  (log/error "Failed to clear file"
                                             {:error (ex-message err)
                                              :file-path file-path})))))


#_(defn dispatch [err {:keys [:contract :event] :as evt}]
  (if err
    (log/error "Error Dispatching" {:err err :event evt} ::event-dispatch)
    (when-not (:disable-dispatch-logging? @web3-events)
      (log/info "Dispatching event" {:err err :event evt} ::event-dispatch)))

  (when (and err
             (fn? (:on-error @web3-events)))
    ((:on-error @web3-events) err evt))

  (when (and (:write-events-into-file? @web3-events)
             (not err))
    (append-line-into-file! (:file-path @web3-events) (str evt)))

  (when (or (not err)
            (and err (:dispatch-on-error? @web3-events)))

    (doall
     (for [callback (vals (get-in @(:callbacks @web3-events) [(:contract-key contract) event]))]
       (callback err evt)))))


#_(defn- create-past-event-filters [events last-block-number]
  (->> events
    (medley/remove-vals #(= (last %) "latest"))
    (medley/map-vals (fn [[contract event filter-opts block-opts]]
                       (let [block-opts (assoc block-opts :to-block last-block-number)]
                         (create-event-filter contract event filter-opts block-opts))))))


#_(defn- create-latest-event-filters [events last-block-number]
  (->> events
    (medley/filter-vals #(or (= (last %) "latest")
                             (= (:to-block (last %)) "latest")))
    (medley/map-vals (fn [[contract event filter-opts]]
                       (let [block-opts {:from-block last-block-number :to-block "latest"}]
                         (create-event-filter contract event filter-opts block-opts
                                              (fn [err event]
                                                (dispatch err (assoc event :latest-event? true)))))))
    doall))


#_(defn- get-block-opts-from-event-filter [event-filter]
  (let [options (aget event-filter "options")
        to-dec (fn [option]
                 (if (and option (string/starts-with? option "0x"))
                   (web3/to-decimal option)
                   option))]
    {:from-block (to-dec (aget options "fromBlock"))
     :to-block (to-dec (aget options "toBlock"))}))


#_(defn- log-event-filters-start! [event-filters]
  (js/setTimeout
    (fn []
      (log/info "Started dispatching event filters:"
                {:event-filters
                 (medley/map-vals (fn [event-filter]
                                    (merge {:id (aget event-filter "filterId")}
                                           (get-block-opts-from-event-filter event-filter)))
                                  event-filters)}))
    700))


#_(defn- start-dispatching-latest-events! [events last-block-number]
  (let [event-filters (create-latest-event-filters events last-block-number)]
    (swap! (:event-filters @web3-events) #(into % event-filters))
    (log-event-filters-start! event-filters)
    event-filters))


#_(defn- dispatch-after-past-events-callbacks! []
  (let [callbacks (get-in @(:callbacks @web3-events) [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-fns (vals callbacks)
        callback-ids (keys callbacks)]
    (doseq [callback callback-fns]
      (callback))
    (unregister-callbacks! callback-ids)))


#_(defn- uninstall-filter [filter]
  (web3-eth/stop-watching!
    filter (fn [err]
             (let [id (aget filter "filterId")]
               (when err
                 (log/error "Error uninstalling event filter" {:error err :filter-id id}))))))


(defn start [{:keys [:events :read-past-events-from-file? :write-events-into-file? :file-path] :as opts
              :or {file-path default-file-path}}]

  #_(when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  #_(let [last-block-number (web3-eth/block-number @web3)
        past-event-filters (if-not read-past-events-from-file?
                             (create-past-event-filters events (dec last-block-number))
                             [])
        past-events-from-file (when read-past-events-from-file?
                                (read-events-from-file file-path))]

    (when write-events-into-file?
      (clear-file! file-path))

    (if-not read-past-events-from-file?
      (do
        (replay-past-events-in-order
          (vals past-event-filters)
          dispatch
          {:on-finish (fn []
                        (dispatch-after-past-events-callbacks!)
                        (start-dispatching-latest-events! events last-block-number))})
        (log-event-filters-start! past-event-filters))
      (js/setTimeout                                        ;; other mount components need to start first
        (fn []
          (doseq [event past-events-from-file]
            (dispatch nil event))
          (start-dispatching-latest-events! events last-block-number))
        0))

    (merge opts {:callbacks (atom {})
                 :event-filters (atom past-event-filters)
                 :past-events-from-file past-events-from-file
                 :file-path file-path})))


(defn stop [web3-events]
  #_(let [filters @(:event-filters @web3-events)]
    (log/warn "Stopping web3-events" {:event-filters (medley/map-vals #(aget % "filterId") filters)})
    (doseq [filter (remove nil? (vals filters))]
      (uninstall-filter filter))))
