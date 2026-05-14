const express = require('express');
const db = require('../db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

router.post('/', authRequired, (req, res) => {
  const userId = req.user.sub;
  const { body_location, pain_scale, duration_hours, notes } = req.body || {};
  if (!body_location || pain_scale == null) {
    return res.status(400).json({ error: 'body_location and pain_scale required' });
  }
  if (pain_scale < 1 || pain_scale > 10) {
    return res.status(400).json({ error: 'pain_scale must be 1-10' });
  }
  const result = db
    .prepare(
      `INSERT INTO symptom_log (user_id, body_location, pain_scale, duration_hours, notes)
       VALUES (?, ?, ?, ?, ?)`
    )
    .run(userId, body_location, pain_scale, duration_hours || null, notes || null);
  const entry = db.prepare('SELECT * FROM symptom_log WHERE id = ?').get(result.lastInsertRowid);
  res.status(201).json({ symptom: entry });
});

router.get('/', authRequired, (req, res) => {
  const userId = req.user.sub;
  const limit = Math.min(parseInt(req.query.limit, 10) || 50, 200);
  const offset = parseInt(req.query.offset, 10) || 0;
  const rows = db
    .prepare(
      'SELECT * FROM symptom_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?'
    )
    .all(userId, limit, offset);
  res.json({ symptoms: rows });
});

router.delete('/:id', authRequired, (req, res) => {
  const userId = req.user.sub;
  db.prepare('DELETE FROM symptom_log WHERE id = ? AND user_id = ?').run(req.params.id, userId);
  res.status(204).end();
});

module.exports = router;
