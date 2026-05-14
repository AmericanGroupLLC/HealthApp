process.env.DB_PATH = ':memory:';
process.env.JWT_SECRET = 'test-jwt-secret-for-jest';

const express = require('express');
const request = require('supertest');
const { isRevoked, revoke } = require('../middleware/tokenBlacklist');
const { registerRules, handleValidationErrors } = require('../middleware/inputValidation');
const { pruneOldAuditLogs } = require('../middleware/auditRetention');

// ── Token Blacklist ──────────────────────────────────────────────────────

describe('tokenBlacklist', () => {
  it('isRevoked returns false for an unknown jti', () => {
    expect(isRevoked('never-seen')).toBe(false);
  });

  it('revoke() causes isRevoked() to return true', () => {
    const jti = 'revokable-jti-1';
    revoke(jti);
    expect(isRevoked(jti)).toBe(true);
  });

  it('handles null/undefined gracefully', () => {
    expect(isRevoked(null)).toBeFalsy();
    expect(isRevoked(undefined)).toBeFalsy();
    revoke(null);
    revoke(undefined);
  });
});

// ── Input Validation (registerRules) ─────────────────────────────────────

describe('inputValidation — registerRules', () => {
  function createApp() {
    const a = express();
    a.use(express.json());
    a.post('/test', registerRules, handleValidationErrors, (_req, res) => {
      res.json({ ok: true });
    });
    return a;
  }

  it('passes with valid input', async () => {
    const res = await request(createApp())
      .post('/test')
      .send({ email: 'good@example.com', password: 'abcdef', name: 'Jo' });

    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  it('rejects an invalid email', async () => {
    const res = await request(createApp())
      .post('/test')
      .send({ email: 'bad-email', password: 'abcdef', name: 'Jo' });

    expect(res.status).toBe(400);
    expect(res.body.errors).toEqual(
      expect.arrayContaining([expect.objectContaining({ path: 'email' })]),
    );
  });

  it('rejects a short password', async () => {
    const res = await request(createApp())
      .post('/test')
      .send({ email: 'ok@example.com', password: 'ab', name: 'Jo' });

    expect(res.status).toBe(400);
    expect(res.body.errors).toEqual(
      expect.arrayContaining([expect.objectContaining({ path: 'password' })]),
    );
  });

  it('rejects an empty name', async () => {
    const res = await request(createApp())
      .post('/test')
      .send({ email: 'ok@example.com', password: 'abcdef', name: '' });

    expect(res.status).toBe(400);
    expect(res.body.errors).toEqual(
      expect.arrayContaining([expect.objectContaining({ path: 'name' })]),
    );
  });
});

// ── Audit Retention ──────────────────────────────────────────────────────

describe('auditRetention — pruneOldAuditLogs', () => {
  it('runs without throwing on an empty audit_log table', () => {
    expect(() => pruneOldAuditLogs()).not.toThrow();
  });
});
