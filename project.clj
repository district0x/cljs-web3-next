(defproject cljs-web3-next "1.0.0"
  :description "ClojureScript Web3 library with swappable backends."
  :url "https://github.com/district0x/cljs-web3-next"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojurescript "1.10.520"]
                 [camel-snake-kebab "0.4.0"]
                 [district0x/bignumber "1.0.3"]
                 ]

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :npm {:dependencies [;; before its in cljsjs
                       [web3 "1.2.0"]]
        :devDependencies [[karma "1.7.1"]
                          [karma-chrome-launcher "2.2.0"]
                          [karma-cli "1.0.1"]
                          [karma-cljs-test "0.1.0"]
                          ;; For deploying
                          [jsedn "0.4.1"]]}

  ;; :source-paths ["src" "test"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.1"]
                                  [district0x/async-helpers "0.1.3"]
                                  ]
                   :plugins [[lein-npm "0.6.2"]
                             [lein-cljsbuild "1.1.7"]
                             [lein-doo "0.1.8"]]
                   :source-paths ["src" "test"]
                   :resource-paths ["resources"]
                   }}

  :cljsbuild {:builds [{:id "nodejs-tests"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "tests-output/node/tests.js"
                                   :output-dir "tests-output/node"
                                   :main "tests.nodejs-runner"
                                   :target :nodejs
                                   :optimizations :none
                                   :external-config {:devtools/config {:features-to-install :all}}}}]}

  )
