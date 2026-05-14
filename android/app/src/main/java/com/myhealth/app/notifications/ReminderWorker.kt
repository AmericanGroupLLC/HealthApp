package com.myhealth.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.myhealth.app.MainActivity

/**
 * WorkManager [Worker] that fires a single health-reminder notification.
 *
 * Input data keys:
 *  - `type`  : one of "hydration", "exercise", "sleep"
 *  - `title` : notification title
 *  - `body`  : notification body text
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "health_reminders"
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }

    override fun doWork(): Result {
        ensureChannel()

        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Health Reminder"
        val body = inputData.getString(KEY_BODY) ?: ""

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            applicationContext, type.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        nm.notify(type.hashCode(), notification)

        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID, "Health Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Hydration, exercise & sleep reminders." }
            nm.createNotificationChannel(channel)
        }
    }
}
