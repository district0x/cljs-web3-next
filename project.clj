(defproject cljs-web3-next "1.0.0"
  :description "ClojureScript web3 library with swappable backends."
  :url "https://github.com/district0x/cljs-web3-next"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojurescript "1.10.520"]
                 [district0x/async-helpers "0.1.2"]
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [medley "1.0.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-logging "1.0.5"]
                 [mount "0.1.16"]
                 ]

  :plugins [[lein-npm "0.6.2"]
            [lein-figwheel "0.5.19"]
            [lein-ancient "0.6.15"]]

  :npm {:dependencies [
                       [ethers "4.0.36"]
                       [web3 "1.2.0"]
                       ["@sentry/node" "4.2.1"]
                       [chalk "2.3.0"]
                       [ws "4.0.0"]
                       ;; ;; For deploying
                       [jsedn "0.4.1"]
                       [truffle-hdwallet-provider "1.0.12"]
                       [dotenv "8.0.0"]
                       ]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4578
             :nrepl-port 7777
             :repl-eval-timeout 120000}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.1"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.1"]
                                  [figwheel-sidecar "0.5.19"]
                                  [org.clojure/tools.reader "1.3.2"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [{:id "dev-server"
                        :source-paths ["src/district/server" "src/district/shared" "src/cljs_web3"]
                        :figwheel {:on-jsload "district.server.dev/on-jsload"}
                        :compiler {:main "district.server.dev"
                                   :output-to "dev-server/web3_next.js"
                                   :output-dir "dev-server"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
