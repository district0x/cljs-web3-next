// 	SPDX-License-Identifier: GPL-3.0-only
pragma solidity ^0.8.0;

contract MyContract {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;
  uint public counter;

  event IncrementCounterEvent(uint counter);
  event SetCounterEvent(uint previousValue, uint newValue);
  /* event SpecialEvent(uint someParam); */

  constructor(uint _counter) {
    counter = _counter;
  }

  function myPlus(uint a, uint b) public pure returns (uint) {
    return a + b;
  }

  function setCounter(uint i) public {
    emit SetCounterEvent(counter, i);
    counter = i;
  }

  function incrementCounter(uint i) public {
    counter += i;
    emit IncrementCounterEvent(counter);
  }

  function doubleIncrementCounter(uint i) public {
    incrementCounter(i);
    incrementCounter(i);
  }

  /* function fireSpecialEvent(uint someParam) public { */
  /*   emit SpecialEvent(someParam); */
  /* } */
}
