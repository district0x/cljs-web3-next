(ns district.shared.smart-contracts)

(def smart-contracts
  {:my-contract {:name "MyContract" :address "0x7001688ad7abe32a7badf02ba5e62aca4c6234b7"}
   :forwarder {:name "Forwarder" :address "0xb59d899823a7d0764562ef15b87b666cfc324f95" :forwards-to :my-contract}
   })
