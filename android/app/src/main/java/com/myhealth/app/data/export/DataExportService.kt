package com.myhealth.app.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.myhealth.app.data.room.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExportService @Inject constructor(
    private val profileDao: ProfileDao,
    private val mealDao: MealDao,
    private val activityDao: ActivityDao,
    private val medicineDao: MedicineDao,
    private val doseLogDao: DoseLogDao,
    private val moodDao: MoodDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val customMealDao: CustomMealDao,
    private val customWorkoutDao: CustomWorkoutDao,
    private val symptomLogDao: SymptomLogDao,
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        root.put("exportDate", isoFmt.format(Date()))

        // Profile
        profileDao.observe().first()?.let { p ->
            root.put("profile", JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("birthDateISO", p.birthDateISO)
                put("sex", p.sex)
                put("heightCm", p.heightCm)
                put("weightKg", p.weightKg)
                put("goal", p.goal)
                put("activityLevel", p.activityLevel)
                put("unitsImperial", p.unitsImperial)
                put("themeMode", p.themeMode)
                put("language", p.language)
                put("updatedAt", p.updatedAt)
            })
        }

        // Meals
        root.put("meals", JSONArray().apply {
            mealDao.observeRecent(Int.MAX_VALUE).first().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("kcal", m.kcal)
                    put("protein", m.protein)
                    put("carbs", m.carbs)
                    put("fat", m.fat)
                    put("barcode", m.barcode)
                    put("consumedAt", m.consumedAt)
                })
            }
        })

        // Activities
        root.put("activities", JSONArray().apply {
            activityDao.observeAll().first().forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("kind", a.kind)
                    put("durationMin", a.durationMin)
                    put("kcalBurned", a.kcalBurned)
                    put("notes", a.notes)
                    put("performedAt", a.performedAt)
                })
            }
        })

        // Medicines
        root.put("medicines", JSONArray().apply {
            medicineDao.observeActive().first().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("dosage", m.dosage)
                    put("unit", m.unit)
                    put("manufacturer", m.manufacturer)
                    put("priceCents", m.priceCents)
                    put("criticalLevel", m.criticalLevel)
                    put("eatWhen", m.eatWhen)
                    put("scheduleJSON", m.scheduleJSON)
                    put("colorHex", m.colorHex)
                    put("notes", m.notes)
                    put("createdAt", m.createdAt)
                    put("archivedAt", m.archivedAt)
                })
            }
        })

        // Moods
        root.put("moods", JSONArray().apply {
            moodDao.observeRecent(Int.MAX_VALUE).first().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("value", m.value)
                    put("note", m.note)
                    put("recordedAt", m.recordedAt)
                })
            }
        })

        // Symptoms
        root.put("symptoms", JSONArray().apply {
            symptomLogDao.getAll().first().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("bodyLocation", s.bodyLocation)
                    put("painScale", s.painScale)
                    put("durationHours", s.durationHours)
                    put("notes", s.notes)
                    put("createdAt", s.createdAt)
                })
            }
        })

        // Exercise logs
        root.put("exercises", JSONArray().apply {
            exerciseLogDao.observeRecent(Int.MAX_VALUE).first().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("exerciseId", e.exerciseId)
                    put("performedAt", e.performedAt)
                    put("setsJSON", e.setsJSON)
                    put("notes", e.notes)
                })
            }
        })

        // Custom meals
        root.put("customMeals", JSONArray().apply {
            customMealDao.observeAll().first().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("componentsJSON", c.componentsJSON)
                    put("createdAt", c.createdAt)
                })
            }
        })

        // Custom workouts
        root.put("customWorkouts", JSONArray().apply {
            customWorkoutDao.observeAll().first().forEach { w ->
                put(JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("exerciseIdsJSON", w.exerciseIdsJSON)
                    put("createdAt", w.createdAt)
                })
            }
        })

        return root.toString(2)
    }

    suspend fun exportToFile(context: Context): Uri {
        val json = exportToJson()
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val fileName = "myhealth-export-$datePart.json"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Failed to open output stream for $uri")

        return uri
    }
}
