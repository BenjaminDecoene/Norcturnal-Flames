package com.example.nocternalflames

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class Foreground: Service() {

    // Local candleState
    private var gameTime: Long = 600900 // 10 minutes
    private var timeLeftInMillis: Long = 601000 // 10 minutes
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler()
    private var fullCandleCountInMillis: Long = 30000
    private var fullCanclesTimeLeft: Long = 30000
    private var countDownTimerFullCancles: CountDownTimer? = null
    private var candleCount =  0
    private var minCandles = 5

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "VibrationServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vibration Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification for the foreground service
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("The app is active in the background")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        // Start the service in the foreground
        startForeground(1, notification)
    }

    // Binder given to clients to call public methods
    private val binder = LocalBinder()

    // Class used for the client Binder.
    inner class LocalBinder : Binder() {
        fun getService(): Foreground = this@Foreground
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun makePhoneBuzz(context: Context, time: Long) {
        // Get the Vibrator service
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Check if the device has a vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android O and above
                val vibrationEffect = VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                // For devices below Android O
                vibrator.vibrate(time) // Vibrates for 500 milliseconds
            }
        }
    }
}