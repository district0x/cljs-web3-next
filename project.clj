(defproject cljs-web3-next "2.0.0"
  :description "ClojureScript Web3 library with swappable backends."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[camel-snake-kebab "0.4.0"]
                 [district0x/bignumber "1.0.3"]
                 [binaryage/oops "0.7.0"]
                 [org.clojure/clojurescript "1.10.520"]]

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :npm {:dependencies [[web3 "1.2.0"]
                       [jsedn "0.4.1"]]
        :devDependencies [[karma "1.7.1"]
                          [karma-chrome-launcher "2.2.0"]
                          [karma-cli "1.0.1"]
                          [karma-cljs-test "0.1.0"]
                          ;; For deploying contracts
                          [jsedn "0.4.1"]]}

  :profiles {:dev {:dependencies [[district0x/async-helpers "0.1.3"]
                                  [day8.re-frame/http-fx "0.1.4"]
                                  [re-frame "0.10.2"]
                                  [district0x/district-ui-web3 "1.0.1"]
                                  [district0x/re-frame-spec-interceptors "1.0.1"]
                                  [day8.re-frame/async-flow-fx "0.0.8"]
                                  [org.clojure/data.json "0.2.6"]
                                  [cljs-ajax "0.7.2"]
                                  [district0x/district-ui-smart-contracts "1.0.9"]
                                  [org.clojure/core.async "0.4.500"]
                                  [day8.re-frame/test "0.1.5"]
                                  [district0x/district-ui-web3-accounts "1.0.3"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [org.clojure/clojure "1.10.1"]]
                   :plugins [[lein-npm "0.6.2"]
                             [lein-cljsbuild "1.1.7"]
                             [lein-doo "0.1.8"]]
                   :source-paths ["src" "test"]
                   :resource-paths ["resources"]}}

  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["deploy"]]

  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "browser-test"]
                        :compiler {:output-to "tests-output/tests.js"
                                   :output-dir "tests-output"
                                   :main "browser-test.runner"
                                   :optimizations :none}}
                       {:id "nodejs-tests"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "tests-output/node/tests.js"
                                   :output-dir "tests-output/node"
                                   :main "tests.nodejs-runner"
                                   :target :nodejs
                                   :optimizations :none
                                   :external-config {:devtools/config {:features-to-install :all}}}}]})
