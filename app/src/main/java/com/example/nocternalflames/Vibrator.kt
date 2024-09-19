package com.example.nocternalflames

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context

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

