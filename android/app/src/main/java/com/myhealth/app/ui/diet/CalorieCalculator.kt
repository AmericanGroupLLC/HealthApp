package com.myhealth.app.ui.diet

import kotlin.math.roundToInt

object CalorieCalculator {

    private val activityFactors = mapOf(
        "sedentary" to 1.2,
        "light" to 1.375,
        "moderate" to 1.55,
        "active" to 1.725,
    )

    private val goalFactors = mapOf(
        "lose" to 0.8,
        "maintain" to 1.0,
        "gain" to 1.15,
    )

    fun dailyTarget(
        sex: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        activityLevel: String,
        goal: String,
    ): Int {
        val bmr = 10 * weightKg + 6.25 * heightCm - 5 * age +
            if (sex.lowercase() == "male") 5 else -161

        val activity = activityFactors[activityLevel.lowercase()] ?: 1.2
        val goalMult = goalFactors[goal.lowercase()] ?: 1.0

        return (bmr * activity * goalMult).roundToInt()
    }
}
