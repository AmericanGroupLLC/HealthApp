package com.myhealth.app.data.testing

import androidx.annotation.VisibleForTesting
import com.myhealth.app.data.room.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
@VisibleForTesting
class TestDataSeeder @Inject constructor(
    private val profileDao: ProfileDao,
    private val mealDao: MealDao,
    private val activityDao: ActivityDao,
    private val moodDao: MoodDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val symptomLogDao: SymptomLogDao,
    private val customMealDao: CustomMealDao,
    private val customWorkoutDao: CustomWorkoutDao,
) {

    suspend fun seedAll() {
        seedProfile()
        seedMeals()
        seedMoodEntries()
        seedSymptomEntries()
        seedExerciseLogs()
    }

    private suspend fun seedProfile() {
        profileDao.upsert(
            ProfileEntity(
                id = "test-user-alpha",
                name = "Test User Alpha",
                birthDateISO = "1996-01-15",
                sex = "male",
                heightCm = 175.0,
                weightKg = 70.0,
                goal = "maintain",
                activityLevel = "moderate",
                unitsImperial = false,
                themeMode = "system",
                language = "en",
            )
        )
    }

    private suspend fun seedMeals() {
        val mealTypes = listOf("Breakfast", "Lunch", "Dinner")
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        for (day in 0 until 30) {
            for ((idx, type) in mealTypes.withIndex()) {
                val kcal = 300.0 + Random.nextInt(500)
                mealDao.insert(
                    MealEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Test $type ${day + 1}",
                        kcal = kcal,
                        protein = kcal * 0.25 / 4,
                        carbs = kcal * 0.50 / 4,
                        fat = kcal * 0.25 / 9,
                        consumedAt = now - (day * dayMs) + (idx * 5 * 60 * 60 * 1000L),
                    )
                )
            }
        }
    }

    private suspend fun seedMoodEntries() {
        val notes = listOf("Feeling great", "A bit tired", "Energized", "Stressed", "Calm and relaxed",
            "Good morning", "Post-workout high", "Need more sleep", "Productive day", "Winding down")
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        for (i in 0 until 10) {
            moodDao.insert(
                MoodEntity(
                    id = UUID.randomUUID().toString(),
                    value = Random.nextInt(1, 6),
                    note = notes[i],
                    recordedAt = now - (i * 3 * dayMs),
                )
            )
        }
    }

    private suspend fun seedSymptomEntries() {
        val symptoms = listOf(
            "Head" to "Mild headache after screen time",
            "Lower back" to "Dull ache after sitting",
            "Right knee" to "Sharp pain during squats",
            "Neck" to "Stiffness in the morning",
            "Left shoulder" to "Soreness after workout",
        )
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        for ((i, pair) in symptoms.withIndex()) {
            symptomLogDao.insert(
                SymptomLogEntity(
                    bodyLocation = pair.first,
                    painScale = Random.nextInt(2, 8),
                    durationHours = Random.nextDouble(0.5, 4.0),
                    notes = pair.second,
                    createdAt = now - (i * 5 * dayMs),
                )
            )
        }
    }

    private suspend fun seedExerciseLogs() {
        val exercises = listOf("bench-press", "squat", "deadlift", "pull-up", "overhead-press")
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        for ((i, exerciseId) in exercises.withIndex()) {
            exerciseLogDao.insert(
                ExerciseLogEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    performedAt = now - (i * 4 * dayMs),
                    setsJSON = """[{"reps":10,"weight":${40 + i * 10}},{"reps":8,"weight":${45 + i * 10}}]""",
                    notes = "Test exercise log ${i + 1}",
                )
            )
        }
    }
}
