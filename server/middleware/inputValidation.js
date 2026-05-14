'use strict';

const { body, validationResult } = require('express-validator');

function handleValidationErrors(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array() });
  }
  next();
}

const registerRules = [
  body('email').isEmail().withMessage('Valid email is required').normalizeEmail(),
  body('password').isLength({ min: 6 }).withMessage('Password must be 6+ chars'),
  body('name').trim().notEmpty().withMessage('Name is required').escape(),
];

const loginRules = [
  body('email').isEmail().withMessage('Valid email is required').normalizeEmail(),
  body('password').notEmpty().withMessage('Password is required'),
];

const profileRules = [
  body('age').optional().isInt({ min: 1, max: 120 }).withMessage('Age must be 1-120'),
  body('height_cm').optional().isFloat({ min: 50, max: 300 }).withMessage('Height must be 50-300cm'),
  body('weight_kg').optional().isFloat({ min: 10, max: 500 }).withMessage('Weight must be 10-500kg'),
  body('sex').optional().isIn(['male', 'female', 'other']).withMessage('Invalid sex'),
  body('goal').optional().trim().escape(),
  body('birth_location').optional().trim().escape(),
];

const mealRules = [
  body('name').trim().notEmpty().withMessage('Meal name is required').escape(),
  body('kcal').isFloat({ min: 0 }).withMessage('kcal must be >= 0'),
  body('protein_g').optional().isFloat({ min: 0 }),
  body('carbs_g').optional().isFloat({ min: 0 }),
  body('fat_g').optional().isFloat({ min: 0 }),
];

const symptomRules = [
  body('body_location').trim().notEmpty().withMessage('Body location is required').escape(),
  body('pain_scale').isInt({ min: 1, max: 10 }).withMessage('Pain scale must be 1-10'),
  body('notes').optional().trim().escape(),
];

const waterRules = [
  body('milliliters').isFloat({ min: 1 }).withMessage('Milliliters must be > 0'),
];

module.exports = {
  handleValidationErrors,
  registerRules,
  loginRules,
  profileRules,
  mealRules,
  symptomRules,
  waterRules,
};
