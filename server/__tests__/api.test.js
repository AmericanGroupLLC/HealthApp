process.env.DB_PATH = ':memory:';
process.env.JWT_SECRET = 'test-jwt-secret-for-jest';

const request = require('supertest');
const { _app: app } = require('../server');

// Advance Date.now() between tests to bypass the auth rate limiter
// (5 requests per IP per 60-second window defined in routes/auth.js).
let currentTime = Date.now();
const _realDateNow = Date.now;
beforeAll(() => { Date.now = () => currentTime; });
afterAll(() => { Date.now = _realDateNow; });
beforeEach(() => { currentTime += 65_000; });

// ── POST /api/auth/register ──────────────────────────────────────────────

describe('POST /api/auth/register', () => {
  it('creates a user with valid data', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'alice@test.com', password: 'secret123', name: 'Alice' });

    expect(res.status).toBe(201);
    expect(res.body.user).toMatchObject({ email: 'alice@test.com', name: 'Alice' });
    expect(res.body.user).toHaveProperty('id');
    expect(res.body).toHaveProperty('token');
  });

  it('returns 400 when fields are missing', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'bob@test.com' });

    expect(res.status).toBe(400);
  });

  it('returns 400 for an invalid email', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'not-an-email', password: 'secret123', name: 'Bad' });

    expect(res.status).toBe(400);
  });

  it('returns 409 for a duplicate email', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'alice@test.com', password: 'other456', name: 'Alice2' });

    expect(res.status).toBe(409);
  });
});

// ── POST /api/auth/login ─────────────────────────────────────────────────

describe('POST /api/auth/login', () => {
  it('succeeds with correct credentials', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'alice@test.com', password: 'secret123' });

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('token');
    expect(res.body.user.email).toBe('alice@test.com');
  });

  it('returns 401 with wrong password', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'alice@test.com', password: 'wrongpass' });

    expect(res.status).toBe(401);
  });

  it('returns 401 for non-existent user', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'nobody@test.com', password: 'whatever' });

    expect(res.status).toBe(401);
  });
});

// ── POST /api/auth/logout + token revocation ─────────────────────────────

describe('POST /api/auth/logout', () => {
  let token;

  beforeAll(async () => {
    currentTime += 65_000;
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'alice@test.com', password: 'secret123' });
    token = res.body.token;
  });

  it('revokes the current token', async () => {
    const res = await request(app)
      .post('/api/auth/logout')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  it('rejects the revoked token on subsequent requests', async () => {
    const res = await request(app)
      .post('/api/auth/logout')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(401);
  });
});

// ── GET /api/profile — auth required ─────────────────────────────────────

describe('GET /api/profile', () => {
  it('returns 401 without a token', async () => {
    const res = await request(app).get('/api/profile');
    expect(res.status).toBe(401);
  });

  it('returns 401 with an invalid token', async () => {
    const res = await request(app)
      .get('/api/profile')
      .set('Authorization', 'Bearer invalid.jwt.token');

    expect(res.status).toBe(401);
  });
});
