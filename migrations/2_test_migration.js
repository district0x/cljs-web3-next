const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const fs = require('fs');
const edn = require("jsedn");
const {contracts_build_directory, smart_contracts_path} = require ('../truffle.js');
// const web3Utils = require('web3-utils');

// copy artifacts for placeholder replacements
copy ("MyContract", "MyContractCp", contracts_build_directory);
const MyContract = artifacts.require("MyContractCp");

copy ("Forwarder", "ForwarderCp", contracts_build_directory);
const Forwarder = artifacts.require("ForwarderCp");

const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";

/**
 * This migration deploys the test smart contract suite
 *
 * Usage:
 * truffle migrate --network ganache --reset --f 10 --to 10
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer
    .then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  })
    .then (() => deployer.deploy (MyContract, 1, Object.assign(opts, {gas: gas})))
    .then (instance => {
      linkBytecode(Forwarder, forwarderTargetPlaceholder, instance.address);
      return deployer.deploy(Forwarder, Object.assign(opts, {gas: gas}))
    })
    .then ((forwader) => Promise.all ([MyContract.deployed (),
                                       forwader
                                      ]))
    .then ((
      [myContract,
       forwarder]) => {

         var smartContracts = edn.encode(
           new edn.Map([

             edn.kw(":my-contract"), new edn.Map([edn.kw(":name"), "MyContract",
                                                  edn.kw(":address"), myContract.address]),

             edn.kw(":forwarder"), new edn.Map([edn.kw(":name"), "Forwarder",
                                                edn.kw(":address"), forwarder.address,
                                                edn.kw(":forwards-to"), edn.kw(":my-contract")])
           ]));

         console.log (smartContracts);
         fs.writeFileSync(smart_contracts_path, smartContractsTemplate (smartContracts, "test"));
       })
    .catch(console.error);

  deployer.then (function () {
    console.log ("Done");
  });

}
