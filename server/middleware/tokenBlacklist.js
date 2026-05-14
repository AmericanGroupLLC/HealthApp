'use strict';

const revokedJtis = new Set();

function isRevoked(jti) {
  return jti && revokedJtis.has(jti);
}

function revoke(jti) {
  if (jti) revokedJtis.add(jti);
}

module.exports = { isRevoked, revoke };
