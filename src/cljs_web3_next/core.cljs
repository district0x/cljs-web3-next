(ns cljs-web3-next.core
  (:require [cljs.nodejs :as nodejs]
            [oops.core :refer [ocall ocall+ oget oget+ gget]]
            [cljs-web3-next.utils :as utils]
            [cljs-web3-next.helpers :as web3-helpers]))

(def Web3 (nodejs/require "web3"))

;; reconsider http-provider args with <1.5.2 web3
(defn http-provider
  ([uri] (http-provider uri Web3))
  ([uri web3] (new web3 (new (aget web3 "providers" "HttpProvider") uri))))

(defn websocket-provider [uri opts]
  (new Web3 (new (aget Web3 "providers" "WebsocketProvider") uri (web3-helpers/cljkk->js opts))))

(defn ws-provider
  ([uri] (ws-provider uri {}))
  ([uri opts] (websocket-provider uri opts)))

(defn connection-url [provider]
  (oget provider "currentProvider" "connection" "_url"))

(defn extend [provider property methods]
  (ocall+ provider "extend" (web3-helpers/cljkk->js {:property property
                                                        :methods methods})))

(defn connected? [provider]
  (oget provider "currentProvider" "connected"))

(defn disconnect [provider]
  (ocall (oget provider "currentProvider") "disconnect"))

(defn on-connect [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "connect" callback])))

(defn on-disconnect [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "end" callback])))

(defn on-error [provider & [callback]]
  (ocall+ (oget provider "currentProvider") (remove nil? ["on" "error" callback])))

;; compatible API polyfills (proxies)

(defn- fallback-web3? []
  (gget ".?web3.?currentProvider"))

;; 'undefined'

(defn default-web3 []
  (new Web3 (gget ".?web3.?currentProvider" )))


(def version-ethereum
  "Returns a hexadecimal string representing the Ethereum protocol version.
  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result
  Example:
  user> `(version-ethereum web3-instance
           (fn [err res] (when-not err (println res))))`
  nil
  user> 0x3f"
  (oget (default-web3) "version"))

(def version-api
  "Returns a hexadecimal string representing the Ethereum protocol version.
  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result
  Example:
  user> `(version-ethereum web3-instance
           (fn [err res] (when-not err (println res))))`
  nil
  user> 0x3f"
  (oget (default-web3) "version"))


(defn sha3
  "Returns a string representing the Keccak-256 SHA3 of the given data.

  Breaking change in 1.0: options are not accepted

  Parameters:
  String - The string to hash using the Keccak-256 SHA3 algorithm
  Map    - (optional) Set encoding to hex if the string to hash is encoded
                      in hex. A leading 0x will be automatically ignored.
  Web3   - (optional first argument) Web3 JavaScript object.

  Example:
  user> (def hash \"Some string to be hashed\")
  #'user/hash
  user> `(sha3 hash)
  \"0xed973b234cf2238052c9ac87072c71bcf33abc1bbd721018e0cca448ef79b379\"`
  user> `(sha3 hash {:encoding :hex})`
  \"0xbd83a94d23235dd7dfcf67a5a0d9e9643a715cd5b528083a2cf944d61f8e7b51\"

  NOTE: This differs from the documented result of the Web3 JavaScript API,
  which equals
  \"0x85dd39c91a64167ba20732b228251e67caed1462d4bcf036af88dc6856d0fdcc\""
  ([string] (utils/sha3 (default-web3) string))
  ([Web3 string]
   (utils/sha3 Web3 string)))

(defn to-hex
  "Returns hexadecimal string representation of any value
  string|number|map|set|BigNumber.

  Parameters:
  Any  - The value to parse
  Web3 - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(to-hex \"foo\")`
  \"0x666f6f\" "
  ([any] (to-hex (default-web3) any))
  ([Web3 any]
   (utils/to-hex Web3 any)))


(defn to-ascii
  "Converts a HEX string into a ASCII string.

  Parameters:
  hex-string - A HEX string to be converted to ASCII.
  Web3       - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(to-ascii \"0x666f6f\")`
  \"foo\" "
  ([hex-string] (to-ascii (default-web3) hex-string))
  ([Web3 hex-string]
   (utils/to-ascii Web3 hex-string)))


(defn from-ascii
  "Converts any ASCII string to a HEX string.

  Parameters:
  string  - An ASCII string to be converted to HEX.
  padding - (optional) The number of bytes the returned HEX string should have.
  Web3    - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(from-ascii \"ethereum\")`
  \"0x657468657265756d\"
  user> `(from-ascii \"ethereum\")`
  \"0x657468657265756d000000000000000000000000000000000000000000000000\"

  NOTE: The latter is intended behaviour. Because of a bug in Web3 the padding
        is not added. See https://github.com/ethereum/web3.js/issues/337"
  ([string] (from-ascii string nil))
  ([string padding] (from-ascii (default-web3)  string padding))
  ([Web3 string padding]
   (ocall+ Web3 "utils" "fromAscii" string padding)))


(defn to-decimal
  "Returns the number representing a HEX string in its number representation.

  renamed to hexToNumber in 1.0

  Parameters:
  hex-string - An HEX string to be converted to a number.
  Web3       - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(to-decimal \"0x15\")`
  21"
  ([hex-string] (to-decimal (default-web3) hex-string))
  ([Web3 hex-string]
   (ocall+ Web3 "utils" "hexToNumber" hex-string)))

(defn from-decimal
  "Converts a number or number string to its HEX representation.

  renamed to numberToHex in 1.0

  Parameters:
  number - A number to be converted to a HEX string.
  Web3   - (optional first argument) Web3 JavaScript object.

  Example:
  user-> `(web3/from-decimal 21)`
  \"0x15\""
  ([number] (from-decimal (default-web3)  number))
  ([Web3 number]
   (ocall+ Web3 "utils" "numberToHex" number)))

(defn from-wei
  "Converts a number of Wei into an Ethereum unit.

  Parameters:
  number - A number or BigNumber instance.
  unit   - One of :noether :wei :kwei :Kwei :babbage :femtoether :mwei :Mwei
           :lovelace :picoether :gwei :Gwei :shannon :nanoether :nano :szabo
           :microether :micro :finney :milliether :milli :ether :kether :grand
           :mether :gether :tether
  Web3   - (optional first argument) Web3 JavaScript object.

  Returns either a number string, or a BigNumber instance, depending on the
  given number parameter.

  Example:
  user> `(web3/from-wei 10 :ether)`
  \"0.00000000000000001\""
  ([number unit] (from-wei (default-web3) number unit))
  ([Web3 number unit]
   (ocall+ Web3 "utils" "fromWei" number (name unit))))

(defn to-wei
  "Converts an Ethereum unit into Wei.

  Parameters:
  number - A number or BigNumber instance.
  unit   - One of :noether :wei :kwei :Kwei :babbage :femtoether :mwei :Mwei
           :lovelace :picoether :gwei :Gwei :shannon :nanoether :nano :szabo
           :microether :micro :finney :milliether :milli :ether :kether :grand
           :mether :gether :tether
  Web3   - (optional first argument) Web3 JavaScript object.

  Returns either a number string, or a BigNumber instance, depending on the
  given number parameter.

  Example:
  user> `(web3/to-wei 10 :ether)`
  \"10000000000000000000\""
  ([number unit] (to-wei (default-web3) number unit))
  ([Web3 number unit]
   (ocall+ Web3 "utils" "toWei" number (name unit))))

(defn to-big-number
  "Converts a given number into a BigNumber instance.

  renamed to toBN in 1.0

  Parameters:
  number-or-hex-string - A number, number string or HEX string of a number.
  Web3                 - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(to-big-number \"10000000000000000000\")`
  <An instance of BigNumber>"
  ([number-or-hex-string] (to-big-number (default-web3) number-or-hex-string))
  ([Web3 number-or-hex-string]
   (ocall+ Web3 "utils" "toBN" number-or-hex-string)))

(defn pad-left
  "Returns input string with zeroes or sign padded to the left.

  Parameters:
  string - String to be padded
  chars  - Amount of chars to address
  sign   - (optional) Char to pad with (behaviour with multiple chars is
                      undefined)
  Web3   - (optional first argument) Web3 JavaScript object.

  Example:
  user> `(web3/pad-left \"foo\" 8)`
  \"00000foo\"
  user> `(web3/pad-left \"foo\" 8 \"b\")`
  \"bbbbbfoo\" "
  ([string chars] (pad-left string chars nil))
  ([string chars sign] (pad-left (default-web3) string chars sign))
  ([Web3 string chars sign]
   (ocall+ Web3 "utils" "padLeft" string chars sign)))

(defn pad-right
  "Returns input string with zeroes or sign padded to the right.

  Parameters:
  string - String to be padded
  chars  - Amount of total chars
  sign   - (optional) Char to pad with (behaviour with multiple chars is
                      undefined)
  Web3   - (optional first argument) Web3 instance

  Example:
  user> `(web3/pad-right \"foo\" 8)`
  \"foo00000\"
  user> `(web3/pad-right \"foo\" 8 \"b\")`
  \"foobbbbb\" "
  ([string chars] (pad-right string chars nil))
  ([string chars sign] (pad-right (default-web3) string chars sign))
  ([Web3 string chars sign]
   (ocall+ Web3 "utils" "padRight" string chars sign)))

(defn address?
  "Returns a boolean indicating if the given string is an address.

  Parameters:
  address - An HEX string.
  Web3    - (Optional first argument) Web3 JavaScript object

  Returns false if it's not on a valid address format. Returns true if it's an
  all lowercase or all uppercase valid address. If it's a mixed case address, it
  checks using web3's isChecksumAddress().

  Example:
  user> `(address? \"0x8888f1f195afa192cfee860698584c030f4c9db1\")`
  true

  ;; With first f capitalized
  user> `(web3/address? \"0x8888F1f195afa192cfee860698584c030f4c9db1\")`
  false"
  ([address] (address? (default-web3) address))
  ([Web3 address]
   (ocall+ Web3 "utils" "isAddress" [address])))

(defn reset
  "Should be called to reset the state of web3. Resets everything except the manager.
  Uninstalls all filters. Stops polling.

  was a breaking change in 1.0, reset() removed shimming to setProvider(currentProvider)

  Parameters:
  web3             - An instance of web3
  keep-is-syncing? - If true it will uninstall all filters, but will keep the
                     web3.eth.isSyncing() polls

  Returns nil.

  Example:
  user> `(reset web3-instance true)`
  nil"
  ([web3]
   (reset web3 false))
  ([web3 keep-is-syncing?]
   (ocall+ web3 ".?setProvider" (oget+ web3 ".?currentProvider"))))

(defn set-provider
  "Should be called to set provider.

  Parameters:
  web3     - Web3 instance
  provider - the provider

  Available providers in web3-cljs:
  - `http-provider`
  - `ipc-provider`

  Example:
  user> `(set-provider web3-instance
                       (http-provider web3-instance \"http://localhost:8545\"))`
  nil"
  [web3 provider]
  (ocall+ web3 "setProvider" provider))

(defn current-provider
  "Will contain the current provider, if one is set. This can be used to check
  if Mist etc. already set a provider.

  Parameters:
  web3 - web3 instance

  Returns the provider set or nil."
  [web3]
  (oget web3 "currentProvider"))

(defn web3
  "Return the web3 instance injected via Mist or Metamask

  Breaking change in MM, MM no longer injects web3
  Shimming to global currentProvider
  "
  []
  (gget ".?web3" ".?currentProvider"))

;;; Providers

(defn ipc-provider [Web3 uri]
  (let [constructor (oget Web3 "providers" "IpcProvider")]
    (constructor. uri)))

(defn create-web3
  "Creates a web3 instance using an `http-provider`.

  Parameters:
  url  - The URL string for which to create the provider.
  Web3 - (Optional first argument) Web3 JavaScript object

  Example:
  user> `(create-web3 \"http://localhost:8545/\")`
  <web3 instance>"
  ([url] (create-web3 (default-web3) url))
  ([Web3 url]
   (new Web3 (http-provider Web3 url))))
