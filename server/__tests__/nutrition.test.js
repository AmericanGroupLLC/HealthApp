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
    .send({ email: 'nutrition@test.com', password: 'secret123', name: 'NutritionUser' });
  token = res.body.token;
});

// ── POST /api/nutrition/meal ──────────────────────────────────────────────

describe('POST /api/nutrition/meal', () => {
  it('logs a meal with macros', async () => {
    const res = await request(app)
      .post('/api/nutrition/meal')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Chicken Salad', kcal: 350, protein_g: 30, carbs_g: 15, fat_g: 18 });

    expect(res.status).toBe(201);
    expect(res.body.meal).toMatchObject({
      name: 'Chicken Salad',
      kcal: 350,
      protein_g: 30,
      carbs_g: 15,
      fat_g: 18,
    });
    expect(res.body.meal).toHaveProperty('id');
  });

  it('returns 400 when name is missing', async () => {
    const res = await request(app)
      .post('/api/nutrition/meal')
      .set('Authorization', `Bearer ${token}`)
      .send({ kcal: 200 });

    expect(res.status).toBe(400);
  });

  it('returns 400 when kcal is missing', async () => {
    const res = await request(app)
      .post('/api/nutrition/meal')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Toast' });

    expect(res.status).toBe(400);
  });

  it('returns 401 without auth', async () => {
    const res = await request(app)
      .post('/api/nutrition/meal')
      .send({ name: 'Salad', kcal: 100 });

    expect(res.status).toBe(401);
  });
});

// ── GET /api/nutrition/today ──────────────────────────────────────────────

describe('GET /api/nutrition/today', () => {
  it('returns today\'s meals and totals', async () => {
    // Log another meal
    await request(app)
      .post('/api/nutrition/meal')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Oatmeal', kcal: 250, protein_g: 8, carbs_g: 45, fat_g: 5 });

    const res = await request(app)
      .get('/api/nutrition/today')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.meals).toBeInstanceOf(Array);
    expect(res.body.totals).toHaveProperty('kcal');
    expect(res.body.totals).toHaveProperty('protein_g');
    expect(res.body.totals).toHaveProperty('carbs_g');
    expect(res.body.totals).toHaveProperty('fat_g');
    expect(res.body.totals.kcal).toBeGreaterThanOrEqual(250);
  });

  it('returns 401 without auth', async () => {
    const res = await request(app).get('/api/nutrition/today');
    expect(res.status).toBe(401);
  });
});

// ── DELETE /api/nutrition/meal/:id ────────────────────────────────────────

describe('DELETE /api/nutrition/meal/:id', () => {
  let mealId;

  beforeAll(async () => {
    currentTime += 65_000;
    const res = await request(app)
      .post('/api/nutrition/meal')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'To Delete', kcal: 100 });
    mealId = res.body.meal.id;
  });

  it('deletes a meal by id', async () => {
    const res = await request(app)
      .delete(`/api/nutrition/meal/${mealId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.deleted).toBe(1);
  });

  it('returns 0 deleted for non-existent id', async () => {
    const res = await request(app)
      .delete('/api/nutrition/meal/99999')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.deleted).toBe(0);
  });
});
