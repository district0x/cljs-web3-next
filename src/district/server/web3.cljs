(ns district.server.web3
  (:require

   [cljs-web3.macros]
   [cljs-web3.core :as web3-core]
   [web3.impl.web3js :as web3js]

   [district.server.config :refer [config]]
   [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)

(defstate web3
  :start (start (merge (:web3 @config)
                       (:web3 (mount/args))))
  :stop (stop web3))

(defn start [{:keys [:port :url] :as opts}]
  (when (and (not port) (not url))
    (throw (js/Error. "You must provide port or url to start the web3 component")))
  (let [uri (if url
              url
              (str "http://127.0.0.1:" port))
        instance (web3js/new)]
    {:instance instance
     :provider (web3-core/websocket-provider instance uri)}))

  (defn stop [web3]
    ::stopped)
