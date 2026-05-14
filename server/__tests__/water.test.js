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
    .send({ email: 'water@test.com', password: 'secret123', name: 'WaterUser' });
  token = res.body.token;
});

// ── POST /api/water ───────────────────────────────────────────────────────

describe('POST /api/water', () => {
  it('logs a water intake entry', async () => {
    const res = await request(app)
      .post('/api/water')
      .set('Authorization', `Bearer ${token}`)
      .send({ milliliters: 500 });

    expect(res.status).toBe(201);
    expect(res.body.water).toMatchObject({ milliliters: 500 });
    expect(res.body.water).toHaveProperty('id');
  });

  it('returns 400 when milliliters is missing', async () => {
    const res = await request(app)
      .post('/api/water')
      .set('Authorization', `Bearer ${token}`)
      .send({});

    expect(res.status).toBe(400);
  });

  it('returns 400 when milliliters is negative', async () => {
    const res = await request(app)
      .post('/api/water')
      .set('Authorization', `Bearer ${token}`)
      .send({ milliliters: -100 });

    expect(res.status).toBe(400);
  });

  it('returns 401 without auth', async () => {
    const res = await request(app)
      .post('/api/water')
      .send({ milliliters: 250 });

    expect(res.status).toBe(401);
  });
});

// ── GET /api/water/today ──────────────────────────────────────────────────

describe('GET /api/water/today', () => {
  it('returns today\'s total', async () => {
    // Log additional water
    await request(app)
      .post('/api/water')
      .set('Authorization', `Bearer ${token}`)
      .send({ milliliters: 300 });

    const res = await request(app)
      .get('/api/water/today')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('total_ml');
    expect(res.body.total_ml).toBeGreaterThanOrEqual(300);
  });

  it('returns 401 without auth', async () => {
    const res = await request(app).get('/api/water/today');
    expect(res.status).toBe(401);
  });
});

// ── GET /api/water/history ────────────────────────────────────────────────

describe('GET /api/water/history', () => {
  it('returns history grouped by day', async () => {
    const res = await request(app)
      .get('/api/water/history')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.history).toBeInstanceOf(Array);
  });

  it('accepts a days query parameter', async () => {
    const res = await request(app)
      .get('/api/water/history?days=3')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.history).toBeInstanceOf(Array);
  });

  it('returns 401 without auth', async () => {
    const res = await request(app).get('/api/water/history');
    expect(res.status).toBe(401);
  });
});
