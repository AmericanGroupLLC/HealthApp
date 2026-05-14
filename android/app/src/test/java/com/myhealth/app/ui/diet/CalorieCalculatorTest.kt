package com.myhealth.app.ui.diet

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {

    // ── Male BMR ─────────────────────────────────────────────────────────

    @Test
    fun `male BMR baseline — 70kg, 175cm, age 25, sedentary, maintain`() {
        // BMR = 10*70 + 6.25*175 - 5*25 + 5 = 1673.75
        // daily = round(1673.75 * 1.2 * 1.0) = 2009
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(2009, result)
    }

    // ── Female BMR ───────────────────────────────────────────────────────

    @Test
    fun `female BMR baseline — 60kg, 165cm, age 30, sedentary, maintain`() {
        // BMR = 10*60 + 6.25*165 - 5*30 + (-161) = 1320.25
        // daily = round(1320.25 * 1.2 * 1.0) = 1584
        val result = CalorieCalculator.dailyTarget(
            sex = "female", weightKg = 60.0, heightCm = 165.0,
            age = 30, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(1584, result)
    }

    // ── Activity level multipliers ───────────────────────────────────────

    @Test
    fun `activity sedentary yields 2009 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(2009, result)
    }

    @Test
    fun `activity light yields 2302 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "light", goal = "maintain",
        )
        assertEquals(2301, result)
    }

    @Test
    fun `activity moderate yields 2594 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "moderate", goal = "maintain",
        )
        assertEquals(2594, result)
    }

    @Test
    fun `activity active yields 2887 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "active", goal = "maintain",
        )
        assertEquals(2887, result)
    }

    @Test
    fun `unknown activity level defaults to sedentary factor`() {
        val known = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        val unknown = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "couch_potato", goal = "maintain",
        )
        assertEquals(known, unknown)
    }

    // ── Goal adjustments ─────────────────────────────────────────────────

    @Test
    fun `goal lose yields 1607 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "lose",
        )
        assertEquals(1607, result)
    }

    @Test
    fun `goal maintain yields 2009 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(2009, result)
    }

    @Test
    fun `goal gain yields 2310 kcal`() {
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "gain",
        )
        assertEquals(2310, result)
    }

    @Test
    fun `unknown goal defaults to maintain factor`() {
        val maintain = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        val unknown = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "bulk",
        )
        assertEquals(maintain, unknown)
    }

    // ── Edge cases ───────────────────────────────────────────────────────

    @Test
    fun `zero weight yields calories from height and age only`() {
        // BMR = 10*0 + 6.25*170 - 5*25 + 5 = 942.5
        // daily = round(942.5 * 1.2) = 1131
        val result = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 0.0, heightCm = 170.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(1131, result)
    }

    @Test
    fun `sex comparison is case-insensitive`() {
        val lower = CalorieCalculator.dailyTarget(
            sex = "male", weightKg = 80.0, heightCm = 180.0,
            age = 30, activityLevel = "moderate", goal = "maintain",
        )
        val upper = CalorieCalculator.dailyTarget(
            sex = "Male", weightKg = 80.0, heightCm = 180.0,
            age = 30, activityLevel = "moderate", goal = "maintain",
        )
        assertEquals(lower, upper)
    }

    @Test
    fun `non-male sex uses female offset`() {
        val female = CalorieCalculator.dailyTarget(
            sex = "female", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        val other = CalorieCalculator.dailyTarget(
            sex = "other", weightKg = 70.0, heightCm = 175.0,
            age = 25, activityLevel = "sedentary", goal = "maintain",
        )
        assertEquals(female, other)
    }
}
