package com.myhealth.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SleepStage(val type: Int, val startTime: Instant, val endTime: Instant)

data class RunSummary(
    val sessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val title: String?,
    val exerciseType: Int,
)

@Singleton
class HealthConnectGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val READ_PERMS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        )

        val WRITE_PERMS: Set<String> = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
        )
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient? by lazy {
        if (isAvailable) HealthConnectClient.getOrCreate(context) else null
    }

    val readPermissions: Set<String> = READ_PERMS

    suspend fun stepsToday(): Long {
        val c = client ?: return 0
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val end = Instant.now()
        val resp = c.readRecords(
            ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
        )
        return resp.records.sumOf { it.count }
    }

    suspend fun latestRestingHR(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                RestingHeartRateRecord::class,
                TimeRangeFilter.between(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.lastOrNull()?.beatsPerMinute?.toDouble()
    }

    suspend fun latestVo2Max(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                Vo2MaxRecord::class,
                TimeRangeFilter.between(Instant.now().minus(30, ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.lastOrNull()?.vo2MillilitersPerMinuteKilogram
    }

    suspend fun latestWeight(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                WeightRecord::class,
                TimeRangeFilter.between(Instant.now().minus(180, ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.lastOrNull()?.weight?.inKilograms
    }

    suspend fun lastNightSleepHours(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                TimeRangeFilter.between(Instant.now().minus(36, ChronoUnit.HOURS), Instant.now())
            )
        )
        val total = resp.records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        return if (total > 0) total / 60.0 else null
    }

    // ─── Wave 1 additions ────────────────────────────────────────────────

    suspend fun readSleepStages(): List<SleepStage> {
        val c = client ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                TimeRangeFilter.between(Instant.now().minus(36, ChronoUnit.HOURS), Instant.now())
            )
        )
        return resp.records.flatMap { session ->
            session.stages.map { stage ->
                SleepStage(stage.stage, stage.startTime, stage.endTime)
            }
        }
    }

    suspend fun readHrvAverage(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                HeartRateVariabilityRmssdRecord::class,
                TimeRangeFilter.between(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now())
            )
        )
        val values = resp.records.map { it.heartRateVariabilityMillis }
        return if (values.isNotEmpty()) values.average() else null
    }

    // MindfulnessSessionRecord is not available in this SDK version; mindfulness write is a no-op.
    suspend fun writeMindfulSession(durationMinutes: Int) { /* no-op */ }

    suspend fun writeExerciseSession(
        exerciseType: Int,
        startTime: Instant,
        endTime: Instant,
        title: String? = null,
    ) {
        val c = client ?: return
        c.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = startTime,
                    endTime = endTime,
                    exerciseType = exerciseType,
                    title = title,
                    startZoneOffset = null,
                    endZoneOffset = null,
                )
            )
        )
    }

    suspend fun readRecentRuns(limit: Int = 20): List<RunSummary> {
        val c = client ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                ExerciseSessionRecord::class,
                TimeRangeFilter.between(Instant.now().minus(90, ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records
            .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING }
            .sortedByDescending { it.startTime }
            .take(limit)
            .map { RunSummary(it.metadata.id, it.startTime, it.endTime, it.title, it.exerciseType) }
    }

    suspend fun writeHydration(milliliters: Double) {
        val c = client ?: return
        val now = Instant.now()
        c.insertRecords(
            listOf(
                HydrationRecord(
                    startTime = now.minus(1, ChronoUnit.MINUTES),
                    endTime = now,
                    volume = Volume.milliliters(milliliters),
                    startZoneOffset = null,
                    endZoneOffset = null,
                )
            )
        )
    }

    suspend fun readHydrationToday(): Double {
        val c = client ?: return 0.0
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val resp = c.readRecords(
            ReadRecordsRequest(
                HydrationRecord::class,
                TimeRangeFilter.between(start, Instant.now())
            )
        )
        return resp.records.sumOf { it.volume.inMilliliters }
    }

    suspend fun readWeightHistory(days: Int = 30): List<Pair<Instant, Double>> {
        val c = client ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                WeightRecord::class,
                TimeRangeFilter.between(Instant.now().minus(days.toLong(), ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.map { it.time to it.weight.inKilograms }
    }

    suspend fun exerciseMinutesToday(): Long {
        val c = client ?: return 0
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val resp = c.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, Instant.now()))
        )
        return resp.records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
    }

    suspend fun latestBloodPressure(): Pair<Double, Double>? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                BloodPressureRecord::class,
                TimeRangeFilter.between(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now())
            )
        )
        val last = resp.records.lastOrNull() ?: return null
        return last.systolic.inMillimetersOfMercury to last.diastolic.inMillimetersOfMercury
    }

    suspend fun latestBloodGlucose(): Double? {
        val c = client ?: return null
        val resp = c.readRecords(
            ReadRecordsRequest(
                BloodGlucoseRecord::class,
                TimeRangeFilter.between(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.lastOrNull()?.level?.inMillimolesPerLiter
    }

    suspend fun readStepsHistory(days: Int = 7): List<Pair<Instant, Long>> {
        val c = client ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                TimeRangeFilter.between(Instant.now().minus(days.toLong(), ChronoUnit.DAYS), Instant.now())
            )
        )
        return resp.records.map { it.startTime to it.count }
    }
}
