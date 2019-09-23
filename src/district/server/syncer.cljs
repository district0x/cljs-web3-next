(ns district.server.syncer
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [district.shared.async-helpers :refer [safe-go <?]]
            [district.server.web3 :refer [web3]]
            [district.server.web3-events :as web3-events]))
