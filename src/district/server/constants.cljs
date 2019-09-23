(ns district.server.constants)

(def web3-events {

                  ;; :meme-auction-factory/meme-auction-canceled-event [:meme-auction-factory-fwd :MemeAuctionCanceledEvent {} {:from-block 0 :to-block "latest"}]

                  :my-contract/set-counter-event [:my-contract :SetCounterEvent]
                  :my-contract/increment-counter-event [:my-contract :IncrementCounterEvent]

                  })
