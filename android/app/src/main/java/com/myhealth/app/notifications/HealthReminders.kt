package com.myhealth.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic health reminders via WorkManager.
 *
 * All work requests are tagged with [TAG] so they can be cancelled in bulk.
 * Each reminder type also gets its own unique work name to allow individual
 * cancellation without affecting the others.
 */
object HealthReminders {

    const val TAG = "health_reminder"

    private const val WORK_HYDRATION = "health_reminder_hydration"
    private const val WORK_EXERCISE = "health_reminder_exercise"
    private const val WORK_SLEEP = "health_reminder_sleep"

    /**
     * Hydration reminder — fires every 2 hours.
     * WorkManager handles Doze / standby for us.
     */
    fun scheduleHydrationReminder(context: Context) {
        val data = workDataOf(
            ReminderWorker.KEY_TYPE to "hydration",
            ReminderWorker.KEY_TITLE to "Time to hydrate! 💧",
            ReminderWorker.KEY_BODY to "Stay on track — drink a glass of water.",
        )
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(2, TimeUnit.HOURS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_HYDRATION,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Exercise reminder — fires once per day.
     * Default preferred workout time is 7 AM; the initial delay is computed
     * to align the first fire with that hour.
     */
    fun scheduleExerciseReminder(context: Context, preferredHour: Int = 7) {
        val initialDelay = delayUntilNextHour(preferredHour)
        val data = workDataOf(
            ReminderWorker.KEY_TYPE to "exercise",
            ReminderWorker.KEY_TITLE to "Ready for today's workout? 🏋️",
            ReminderWorker.KEY_BODY to "Your daily exercise session is waiting.",
        )
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_EXERCISE,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Sleep reminder — fires once per day, 30 minutes before the user's
     * target bedtime. Defaults to 10 PM (22:00) bedtime → fires at 9:30 PM.
     */
    fun scheduleSleepReminder(context: Context, bedtimeHour: Int = 22, bedtimeMinute: Int = 0) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, bedtimeMinute)
            add(Calendar.MINUTE, -30)
        }
        val reminderHour = cal.get(Calendar.HOUR_OF_DAY)
        val reminderMinute = cal.get(Calendar.MINUTE)

        val initialDelay = delayUntilNextTime(reminderHour, reminderMinute)
        val data = workDataOf(
            ReminderWorker.KEY_TYPE to "sleep",
            ReminderWorker.KEY_TITLE to "Wind down time 🌙",
            ReminderWorker.KEY_BODY to "Your sleep goal is in 30 minutes.",
        )
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_SLEEP,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelHydration(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_HYDRATION)
    }

    fun cancelExercise(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_EXERCISE)
    }

    fun cancelSleep(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_SLEEP)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun delayUntilNextHour(hour: Int): Long = delayUntilNextTime(hour, 0)

    private fun delayUntilNextTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
