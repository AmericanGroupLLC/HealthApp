package com.myhealth.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.myhealth.app.notifications.MedicineReminderScheduler
import com.myhealth.app.notifications.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt entry point. Bootstraps WorkManager and re-syncs medicine reminders
 * the first time the process starts.
 */
@HiltAndroidApp
class MyHealthApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var medicineScheduler: MedicineReminderScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Crash reporting — opt-in via Settings + DSN must be configured.
        com.myhealth.app.crash.CrashReportingService.bootstrapIfEnabled(
            context = this,
            releaseName = "MyHealth-Android@${BuildConfig.VERSION_NAME ?: "1.0"}"
        )
        // Product analytics — same opt-in pattern, separate API key.
        com.myhealth.app.analytics.AnalyticsService.bootstrapIfEnabled(this)
        // Re-arm reminders for any active medicine on cold start.
        medicineScheduler.resyncAll()
        // Create the health-reminders notification channel early.
        createHealthReminderChannel()
    }

    private fun createHealthReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(ReminderWorker.CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    ReminderWorker.CHANNEL_ID, "Health Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Hydration, exercise & sleep reminders." }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
