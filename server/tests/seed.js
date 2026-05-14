const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');

const JWT_SECRET = process.env.JWT_SECRET || 'test-jwt-secret-for-jest';

module.exports = function seedTestData(db) {
  const password = 'testpass123';
  const hash = bcrypt.hashSync(password, 4); // low rounds for speed in tests

  const userResult = db
    .prepare('INSERT INTO users (email, password, name) VALUES (?, ?, ?)')
    .run('seeduser@test.com', hash, 'Seed User');

  const userId = userResult.lastInsertRowid;

  // Profile
  db.prepare(
    `INSERT INTO profiles (user_id, age, sex, height_cm, weight_kg, activity_level, goal)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).run(userId, 30, 'male', 175, 70, 'moderate', 'maintain');

  // Meals (30 entries over 30 days)
  const mealTypes = ['Breakfast', 'Lunch', 'Dinner'];
  const insertMeal = db.prepare(
    `INSERT INTO meals (user_id, name, kcal, protein_g, carbs_g, fat_g, recorded_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  );
  for (let day = 0; day < 30; day++) {
    const type = mealTypes[day % 3];
    const kcal = 300 + Math.floor(Math.random() * 500);
    const date = new Date(Date.now() - day * 86400000);
    insertMeal.run(
      userId,
      `Test ${type} ${day + 1}`,
      kcal,
      +(kcal * 0.25 / 4).toFixed(1),
      +(kcal * 0.50 / 4).toFixed(1),
      +(kcal * 0.25 / 9).toFixed(1),
      date.toISOString()
    );
  }

  // Water logs (14 entries)
  const insertWater = db.prepare(
    `INSERT INTO water_log (user_id, milliliters, logged_at) VALUES (?, ?, ?)`
  );
  for (let day = 0; day < 14; day++) {
    const ml = 1500 + Math.floor(Math.random() * 1500);
    const date = new Date(Date.now() - day * 86400000);
    insertWater.run(userId, ml, date.toISOString());
  }

  // Symptom logs (5 entries)
  const symptoms = [
    { location: 'Head', scale: 3, notes: 'Mild headache after screen time' },
    { location: 'Lower back', scale: 5, notes: 'Dull ache after sitting' },
    { location: 'Right knee', scale: 6, notes: 'Sharp pain during squats' },
    { location: 'Neck', scale: 4, notes: 'Stiffness in the morning' },
    { location: 'Left shoulder', scale: 3, notes: 'Soreness after workout' },
  ];
  const insertSymptom = db.prepare(
    `INSERT INTO symptom_log (user_id, body_location, pain_scale, duration_hours, notes, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  );
  symptoms.forEach((s, i) => {
    const date = new Date(Date.now() - i * 5 * 86400000);
    insertSymptom.run(
      userId,
      s.location,
      s.scale,
      +(Math.random() * 3.5 + 0.5).toFixed(1),
      s.notes,
      date.toISOString()
    );
  });

  // Token
  const user = db
    .prepare('SELECT id, email, name, created_at FROM users WHERE id = ?')
    .get(userId);

  const token = jwt.sign(
    { sub: user.id, email: user.email, name: user.name, jti: crypto.randomUUID() },
    JWT_SECRET,
    { expiresIn: '1d' }
  );

  return { userId, email: user.email, password, token };
};
