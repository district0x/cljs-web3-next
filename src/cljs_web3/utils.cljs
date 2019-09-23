(ns cljs-web3.utils
  (:require [cljs-web3.api :as api]
            [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.string :as string]))

(defn sha3 [{:keys [:instance :provider]} args]
  (api/-sha3 instance provider args))

(defn solidity-sha3 [{:keys [:instance :provider]} args]
  (api/-solidity-sha3 instance provider args))

(defn safe-case [case-f]
  (fn [x]
    (cond-> (subs (name x) 1)
      true (string/replace "_" "*")
      true case-f
      true (string/replace "*" "_")
      true (->> (str (first (name x))))
      (keyword? x) keyword)))

(def camel-case (safe-case camel-snake/->camelCase))
(def kebab-case (safe-case camel-snake/->kebab-case))

(def pascal-case
  ":base -> :Base"
  (safe-case camel-snake/->PascalCase))

(def js->cljk #(js->clj % :keywordize-keys true))

(def js->cljkk
  "From JavaScript to Clojure with kebab-cased keywords."
  (comp (partial camel-snake-extras/transform-keys kebab-case) js->cljk))

(def cljkk->js
  "From Clojure with kebab-cased keywords to JavaScript e.g.
  {:from-block 0 :to-block 'latest'} -> #js {:fromBlock 0, :toBlock 'latest'}"
  (comp clj->js (partial camel-snake-extras/transform-keys camel-case)))

(defn event-interface [contract-instance event-key]
  (reduce (fn [_ element]
            (when (= (:name element) (-> event-key camel-case name))
              (reduced element)))
          (js->cljk (aget contract-instance "_jsonInterface") )))
