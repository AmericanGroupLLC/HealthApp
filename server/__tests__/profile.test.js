process.env.DB_PATH = ':memory:';
process.env.JWT_SECRET = 'test-jwt-secret-for-jest';

const request = require('supertest');
const { _app: app } = require('../server');

let currentTime = Date.now();
const _realDateNow = Date.now;
beforeAll(() => { Date.now = () => currentTime; });
afterAll(() => { Date.now = _realDateNow; });
beforeEach(() => { currentTime += 65_000; });

let token;

beforeAll(async () => {
  const res = await request(app)
    .post('/api/auth/register')
    .send({ email: 'profile@test.com', password: 'secret123', name: 'ProfileUser' });
  token = res.body.token;
});

// ── GET /api/profile ──────────────────────────────────────────────────────

describe('GET /api/profile', () => {
  it('returns 401 without a token', async () => {
    const res = await request(app).get('/api/profile');
    expect(res.status).toBe(401);
  });

  it('returns profile data for authenticated user', async () => {
    const res = await request(app)
      .get('/api/profile')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.user).toMatchObject({ email: 'profile@test.com', name: 'ProfileUser' });
    expect(res.body).toHaveProperty('profile');
    expect(res.body).toHaveProperty('bmi');
  });
});

// ── PUT /api/profile ──────────────────────────────────────────────────────

describe('PUT /api/profile', () => {
  it('creates a profile on first PUT', async () => {
    const res = await request(app)
      .put('/api/profile')
      .set('Authorization', `Bearer ${token}`)
      .send({ age: 30, sex: 'male', height_cm: 180, weight_kg: 75 });

    expect(res.status).toBe(200);
    expect(res.body.profile).toMatchObject({ age: 30, sex: 'male', height_cm: 180, weight_kg: 75 });
    expect(res.body.bmi).toBeDefined();
    expect(res.body.bmi.value).toBeCloseTo(23.1, 1);
    expect(res.body.bmi.category).toBe('Healthy');
  });

  it('updates existing profile fields', async () => {
    const res = await request(app)
      .put('/api/profile')
      .set('Authorization', `Bearer ${token}`)
      .send({ weight_kg: 80 });

    expect(res.status).toBe(200);
    expect(res.body.profile.weight_kg).toBe(80);
    expect(res.body.profile.height_cm).toBe(180); // unchanged
  });

  it('returns 401 without a token', async () => {
    const res = await request(app)
      .put('/api/profile')
      .send({ age: 25 });

    expect(res.status).toBe(401);
  });
});

// ── POST /api/profile/metrics ─────────────────────────────────────────────

describe('POST /api/profile/metrics', () => {
  it('logs a metric', async () => {
    const res = await request(app)
      .post('/api/profile/metrics')
      .set('Authorization', `Bearer ${token}`)
      .send({ type: 'weight', value: 75.5, unit: 'kg' });

    expect(res.status).toBe(201);
    expect(res.body.metric).toMatchObject({ type: 'weight', value: 75.5, unit: 'kg' });
    expect(res.body.metric).toHaveProperty('id');
  });

  it('returns 400 when type or value is missing', async () => {
    const res = await request(app)
      .post('/api/profile/metrics')
      .set('Authorization', `Bearer ${token}`)
      .send({ type: 'weight' });

    expect(res.status).toBe(400);
  });
});

// ── GET /api/profile/metrics ──────────────────────────────────────────────

describe('GET /api/profile/metrics', () => {
  it('returns all metrics for user', async () => {
    const res = await request(app)
      .get('/api/profile/metrics')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.metrics).toBeInstanceOf(Array);
    expect(res.body.metrics.length).toBeGreaterThanOrEqual(1);
  });

  it('filters by type', async () => {
    // Log a steps metric first
    await request(app)
      .post('/api/profile/metrics')
      .set('Authorization', `Bearer ${token}`)
      .send({ type: 'steps', value: 8000 });

    const res = await request(app)
      .get('/api/profile/metrics?type=steps')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.metrics.every(m => m.type === 'steps')).toBe(true);
  });
});
