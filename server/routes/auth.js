const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const db = require('../db');
const { revoke } = require('../middleware/tokenBlacklist');
const { authRequired } = require('../middleware/auth');

const { registerRules, loginRules, handleValidationErrors } = require('../middleware/inputValidation');

const router = express.Router();

// HIPAA: simple in-memory rate limiter for auth endpoints (5 attempts per IP per minute).
const authAttempts = new Map();
function rateLimit(req, res, next) {
  const key = req.ip || 'unknown';
  const now = Date.now();
  const window = 60_000;
  const maxAttempts = 5;
  const attempts = authAttempts.get(key) || [];
  const recent = attempts.filter(t => now - t < window);
  if (recent.length >= maxAttempts) {
    return res.status(429).json({ error: 'Too many attempts. Try again in 1 minute.' });
  }
  recent.push(now);
  authAttempts.set(key, recent);
  next();
}

router.use(rateLimit);

function signToken(user) {
  return jwt.sign(
    { sub: user.id, email: user.email, name: user.name, jti: crypto.randomUUID() },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '1d' }
  );
}

function publicUser(u) {
  return { id: u.id, email: u.email, name: u.name, created_at: u.created_at };
}

// POST /api/auth/register
router.post('/register', registerRules, handleValidationErrors, async (req, res) => {
  const { email, password, name } = req.body || {};
  if (!email || !password || !name)
    return res.status(400).json({ error: 'email, password, name required' });
  if (password.length < 6)
    return res.status(400).json({ error: 'password must be 6+ chars' });

  const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(email);
  if (existing) return res.status(409).json({ error: 'Email already registered' });

  const hash = await bcrypt.hash(password, 10);
  const result = db
    .prepare('INSERT INTO users (email, password, name) VALUES (?, ?, ?)')
    .run(email, hash, name);

  const user = db
    .prepare('SELECT id, email, name, created_at FROM users WHERE id = ?')
    .get(result.lastInsertRowid);

  res.status(201).json({ user: publicUser(user), token: signToken(user) });
});

// POST /api/auth/login
router.post('/login', loginRules, handleValidationErrors, async (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password)
    return res.status(400).json({ error: 'email and password required' });

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
  if (!user) return res.status(401).json({ error: 'Invalid credentials' });

  const ok = await bcrypt.compare(password, user.password);
  if (!ok) return res.status(401).json({ error: 'Invalid credentials' });

  res.json({ user: publicUser(user), token: signToken(user) });
});

// POST /api/auth/logout — revoke the current JWT
router.post('/logout', authRequired, (req, res) => {
  const jti = req.user && req.user.jti;
  if (jti) revoke(jti);
  res.json({ ok: true });
});

module.exports = router;
