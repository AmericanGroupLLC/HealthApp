const express = require('express');
const db = require('../db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

router.post('/', authRequired, (req, res) => {
  const userId = req.user.sub;
  const { milliliters } = req.body || {};
  if (!milliliters || milliliters <= 0) {
    return res.status(400).json({ error: 'milliliters required and must be positive' });
  }
  const result = db
    .prepare('INSERT INTO water_log (user_id, milliliters) VALUES (?, ?)')
    .run(userId, milliliters);
  const entry = db.prepare('SELECT * FROM water_log WHERE id = ?').get(result.lastInsertRowid);
  res.status(201).json({ water: entry });
});

router.get('/today', authRequired, (req, res) => {
  const userId = req.user.sub;
  const row = db
    .prepare(
      `SELECT COALESCE(SUM(milliliters), 0) AS total_ml
       FROM water_log
       WHERE user_id = ? AND logged_at >= date('now')`
    )
    .get(userId);
  res.json({ total_ml: row.total_ml });
});

router.get('/history', authRequired, (req, res) => {
  const userId = req.user.sub;
  const days = Math.min(parseInt(req.query.days, 10) || 7, 90);
  const rows = db
    .prepare(
      `SELECT date(logged_at) AS day, SUM(milliliters) AS total_ml
       FROM water_log
       WHERE user_id = ? AND logged_at >= date('now', ? || ' days')
       GROUP BY date(logged_at)
       ORDER BY day DESC`
    )
    .all(userId, -days);
  res.json({ history: rows });
});

module.exports = router;
