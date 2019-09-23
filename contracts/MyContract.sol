pragma solidity ^0.4.18;

contract MyContract {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;
  uint public counter;

  event IncrementCounterEvent(uint counter);
  event SetCounterEvent(uint previousValue, uint newValue);
  /* event SpecialEvent(uint someParam); */

  constructor(uint _counter) public {
    counter = _counter;
  }

  function myPlus(uint a, uint b) public constant returns (uint) {
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
