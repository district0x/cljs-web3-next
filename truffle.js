'use strict';

const smartContractsPaths = '/test/tests/smart_contracts_test.cljs';

module.exports = {
  smart_contracts_path: __dirname + smartContractsPaths,
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  networks: {
    ganache: {
      host: 'localhost',
      port: 8549,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    }
  },
  compilers: {
    solc: {
      version: "0.4.23"
    }
  }
};
