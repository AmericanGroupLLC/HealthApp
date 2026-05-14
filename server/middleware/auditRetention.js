'use strict';

const db = require('../db');

const RETENTION_YEARS = 6;

function pruneOldAuditLogs() {
  try {
    const result = db.prepare(
      `DELETE FROM audit_log WHERE created_at < datetime('now', ?)`
    ).run(`-${RETENTION_YEARS} years`);
    if (result.changes > 0) {
      console.log(`[audit_retention] Pruned ${result.changes} audit log entries older than ${RETENTION_YEARS} years.`);
    }
  } catch (err) {
    console.warn('[audit_retention] Prune failed:', err.message);
  }
}

module.exports = { pruneOldAuditLogs };
