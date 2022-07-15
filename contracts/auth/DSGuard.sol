// guard.sol -- simple whitelist implementation of DSAuthority
// 	SPDX-License-Identifier: GPL-3.0-only
pragma solidity ^0.8.0;

import "../auth/DSAuth.sol";

contract DSGuardEvents {
  event LogPermit(
    bytes32 indexed src,
    bytes32 indexed dst,
    bytes32 indexed sig
  );
  event LogForbid(
    bytes32 indexed src,
    bytes32 indexed dst,
    bytes32 indexed sig
  );
}

contract DSGuard is DSAuth, DSAuthority, DSGuardEvents {
  bytes32 constant public ANY = bytes32(uint256(0x01));
  mapping(bytes32 => mapping(bytes32 => mapping(bytes32 => bool))) acl;

  function toBytes(address addr) public pure returns (bytes32) {
    return bytes32(uint256(uint160(addr)) << 96);
  }

  function canCall(
    address src_, address dst_, bytes4 sig
  ) public view override returns (bool) {
    bytes32 src = toBytes(src_);
    bytes32 dst = toBytes(dst_);

    return acl[src][dst][sig]
    || acl[src][dst][ANY]
    || acl[src][ANY][sig]
    || acl[src][ANY][ANY]
    || acl[ANY][dst][sig]
    || acl[ANY][dst][ANY]
    || acl[ANY][ANY][sig]
    || acl[ANY][ANY][ANY];
  }

  function permit(bytes32 src, bytes32 dst, bytes32 sig) public auth {
    acl[src][dst][sig] = true;
    LogPermit(src, dst, sig);
  }

  function forbid(bytes32 src, bytes32 dst, bytes32 sig) public auth {
    acl[src][dst][sig] = false;
    LogForbid(src, dst, sig);
  }

  function permit(address src, address dst, bytes32 sig) public {
    permit(toBytes(src), toBytes(dst), sig);
  }

  function forbid(address src, address dst, bytes32 sig) public {
    forbid(toBytes(src), toBytes(dst), sig);
  }
}

contract DSGuardFactory {
  mapping(address => bool)  public  isGuard;

  function newGuard() public returns (DSGuard guard) {
    guard = new DSGuard();
    guard.setOwner(msg.sender);
    isGuard[address(guard)] = true;
  }
}
