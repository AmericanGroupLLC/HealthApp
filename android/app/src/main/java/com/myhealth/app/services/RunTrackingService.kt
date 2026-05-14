package com.myhealth.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RunTrackingService : Service() {

    private val binder = RunBinder()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    inner class RunBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildNotification("Running..."))
        job = scope.launch {
            while (_isRunning.value) {
                delay(1000)
                _elapsedSeconds.value++
                updateNotification()
            }
        }
    }

    private fun pauseTracking() {
        _isRunning.value = false
        job?.cancel()
        updateNotification()
    }

    private fun stopTracking() {
        _isRunning.value = false
        job?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Run Tracking", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyHealth Run")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun updateNotification() {
        val elapsed = _elapsedSeconds.value
        val text = if (_isRunning.value) {
            "Running — %d:%02d".format(elapsed / 60, elapsed % 60)
        } else {
            "Paused — %d:%02d".format(elapsed / 60, elapsed % 60)
        }
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.myhealth.app.RUN_START"
        const val ACTION_PAUSE = "com.myhealth.app.RUN_PAUSE"
        const val ACTION_STOP = "com.myhealth.app.RUN_STOP"
        const val CHANNEL_ID = "run_tracking"
        const val NOTIFICATION_ID = 42
    }
}
