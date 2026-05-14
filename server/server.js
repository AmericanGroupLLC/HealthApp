require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const Sentry = require('./middleware/sentry');
const Analytics = require('./middleware/analytics');
const auditLog = require('./middleware/auditLog');
const { pruneOldAuditLogs } = require('./middleware/auditRetention');

const authRoutes = require('./routes/auth');
const profileRoutes = require('./routes/profile');
const healthRoutes = require('./routes/health');
const nutritionRoutes = require('./routes/nutrition');
const insightsRoutes = require('./routes/insights');
const socialRoutes = require('./routes/social');
const medicineRoutes = require('./routes/medicine');
// ─── Wave 1: Symptom & Water tracking ────────────────────────────────────
const symptomsRoutes = require('./routes/symptoms');
const waterRoutes = require('./routes/water');
// ─── Care+ v1 (Week 1) ─────────────────────────────────────────────────
const fhirRoutes = require('./routes/fhir');
const vendorRoutes = require('./routes/vendor');
const doctorsRoutes = require('./routes/doctors');
const insuranceRoutes = require('./routes/insurance');

const app = express();

// Initialize Sentry as early as possible. No-op if SENTRY_DSN env var
// is unset, so local dev / CI smoke runs aren't affected.
Sentry.init({
  serviceName: 'myhealth-server',
  release: `MyHealth-Server@${require('./package.json').version}`,
});
Sentry.requestHandler(app);

// PostHog analytics — same opt-in pattern (no-op without POSTHOG_API_KEY).
Analytics.init({
  release: `MyHealth-Server@${require('./package.json').version}`,
});

// HIPAA: restrict CORS to known origins (mobile apps + local dev).
const allowedOrigins = (process.env.CORS_ORIGINS || 'http://localhost:3000,http://localhost:19006').split(',');
app.use(cors({
  origin(origin, cb) {
    if (!origin || allowedOrigins.includes(origin)) cb(null, true);
    else cb(new Error('CORS origin not allowed'));
  },
  credentials: true,
}));
app.use(express.json({ limit: '1mb' }));
app.use(compression());
app.use(helmet());

// HIPAA: enforce HTTPS in production via reverse-proxy header.
if (process.env.NODE_ENV === 'production') {
  app.use((req, res, next) => {
    if (req.headers['x-forwarded-proto'] !== 'https') {
      return res.redirect(301, `https://${req.headers.host}${req.url}`);
    }
    next();
  });
  app.set('trust proxy', 1);
}
app.use((_req, res, next) => {
  res.set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  res.set('X-Content-Type-Options', 'nosniff');
  res.set('X-Frame-Options', 'DENY');
  next();
});

// HIPAA: prevent caching of any PHI-bearing responses.
const noCacheRoutes = ['/api/fhir', '/api/insurance', '/api/health', '/api/profile', '/api/symptoms', '/api/insights', '/api/nutrition'];
noCacheRoutes.forEach(route => {
  app.use(route, (_req, res, next) => { res.set('Cache-Control', 'no-store'); next(); });
});

app.get('/api/health-check', (_req, res) =>
  res.json({ ok: true, service: 'MyHealth API', time: new Date().toISOString() })
);

app.get('/health', (_req, res) =>
  res.json({
    status: 'ok',
    uptime: process.uptime(),
    version: require('./package.json').version,
  })
);

app.use('/api/auth', authRoutes);
app.use('/api/health', healthRoutes);
app.use('/api/medicine', medicineRoutes);

// ─── Audit-logged routes ─────────────────────────────────────────────
//
// All authenticated routes that read/write user data are wrapped in
// `auditLog` so the `audit_log` table records who-did-what-when.
app.use('/api/profile', auditLog, profileRoutes);
app.use('/api/nutrition', auditLog, nutritionRoutes);
app.use('/api/insights', auditLog, insightsRoutes);
app.use('/api/social', auditLog, socialRoutes);
app.use('/api/symptoms', auditLog, symptomsRoutes);
app.use('/api/water', auditLog, waterRoutes);
app.use('/api/fhir', auditLog, fhirRoutes);
app.use('/api/vendor', auditLog, vendorRoutes);
app.use('/api/doctors', auditLog, doctorsRoutes);
app.use('/api/insurance', auditLog, insuranceRoutes);

// HIPAA: prune audit logs older than 6 years on startup.
pruneOldAuditLogs();

// Sentry error handler MUST come before any other error middleware.
Sentry.errorHandler(app);

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Server error' });
});

const PORT = process.env.PORT || 4000;

// Only start the HTTP listener when this file is the entrypoint (`node server.js`
// or `npm run dev`). Tests `require('./server.js')` and access `_app` directly.
let server;
if (require.main === module) {
  server = app.listen(PORT, () => {
    console.log(`✅ MyHealth API listening on http://localhost:${PORT}`);
  });
}

const shutdown = () => {
  if (server) {
    server.close(() => {
      const db = require('./db');
      db.close();
      process.exit(0);
    });
  } else {
    process.exit(0);
  }
};
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

module.exports = { _app: app };
