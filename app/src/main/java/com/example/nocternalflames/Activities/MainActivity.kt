package com.example.nocternalflames.Activities

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.example.nocternalflames.Foreground
import com.example.nocternalflames.LightEvent
import com.example.nocternalflames.R
import com.example.nocternalflames.makePhoneBuzz
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database


class MainActivity : AppCompatActivity(){
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

    private var mService: Foreground? = null
    private var mBound: Boolean = false

    // Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to VibrationService, cast the IBinder and get VibrationService instance
            val binder = service as Foreground.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.main_activity_layout)

        // Setup foreground service
        val intent = Intent(this, Foreground::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Firebase realtime database
        val database = Firebase.database("https://nocturnal-lights-61563-default-rtdb.europe-west1.firebasedatabase.app")

        // Init candle image views
        val candles = listOf(
            findViewById<ImageView>(R.id.candle1),
            findViewById<ImageView>(R.id.candle2),
            findViewById<ImageView>(R.id.candle3),
            findViewById<ImageView>(R.id.candle4),
            findViewById<ImageView>(R.id.candle5)
                )

        val lightEvents = database.getReference("lightEvents")
        lightEvents.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                var count = 0
                var lastCount = 0
                var countdownStartTimer = 0L
                // Count lit candles based on events
                for (snapshot in dataSnapshot.children) {
                    val lightEvent = snapshot.getValue(LightEvent::class.java)
                    val newCount = count + lightEvent!!.value
                    if (newCount < 0 || newCount > candles.size)
                        continue

                    count = newCount

                    // save the timestamp of when the candles go from 4 to 5
                    if(count == minCandles && lastCount == minCandles - 1){
                        countdownStartTimer = lightEvent.timestamp
                    }
                    lastCount = count
                }
                candleCount = count

                // Start countdown if there are 5 or more candles
                if(candleCount >= 5) {
                    startFullCandleTimer(countdownStartTimer)
                    println("Counting down")
                }else{
                    stopFullCandleTimer()
                }

                // Extinguish all candles
                candles.forEach {
                    lightCandle(it, false)
                }

                // Light candles based on count
                for (i in 0..count - 1){
                    lightCandle(candles[i], true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

        // Light candle button
        val lightCandleButton = findViewById<Button>(R.id.lightCandleButton)
        lightCandleButton.setOnClickListener {
            if(countDownTimer == null)
                return@setOnClickListener

            val event = LightEvent(System.currentTimeMillis(), 1)

            val itemsRef = database.getReference("lightEvents")

            val newItemRef = itemsRef.push()
            newItemRef.setValue(event)

            // Disable the button
            lightCandleButton.isEnabled = false

            // Re-enable the button after 10 seconds (10000 milliseconds)
            handler.postDelayed({ lightCandleButton.isEnabled = true }, 10000)
        }

        // Extinguish candle button
        val extinguishCandleButton = findViewById<Button>(R.id.extinguishCandleButton)
        extinguishCandleButton.setOnClickListener {
            if(countDownTimer == null)
                return@setOnClickListener

            val event = LightEvent(System.currentTimeMillis(), -1)
            val itemsRef = database.getReference("lightEvents")

            val newItemRef = itemsRef.push()
            newItemRef.setValue(event)

            // Disable the button
            extinguishCandleButton.isEnabled = false

            // Re-enable the button after 10 seconds (10000 milliseconds)
            handler.postDelayed({ extinguishCandleButton.isEnabled = true }, 10000)
        }

        // Start game button
        val startButton = findViewById<Button>(R.id.startButton)
        val startTimeRef = database.getReference("start time")

        startButton.setOnClickListener {
            startTimeRef.setValue(System.currentTimeMillis())
        }

        // Reset game button
        val resetButton = findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
            resetGame()
        }

        // Listen to candle values in the db
        startTimeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val targetTime = dataSnapshot.value as Long + gameTime
                if(targetTime > System.currentTimeMillis()) {
                    timeLeftInMillis = targetTime - System.currentTimeMillis()
                    countDownTimer?.cancel()
                    startTimer()
                }
                else{
                    // if the game has not started yet
                    countDownTimer?.cancel()
                    timeLeftInMillis = gameTime
                    updateTimer()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun lightCandle(candle:ImageView, light: Boolean){
        if (light)
            candle.setImageResource(R.drawable.candle_on)
        else
            candle.setImageResource(R.drawable.candle_off)
    }

    private fun startTimer() {
        val context = this
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimer()
            }

            override fun onFinish() {
                // Timer finished
                resetGame()
                val intent = Intent(context, SeekersWonActivity::class.java)
                startActivity(intent)
            }
        }.start()
    }

    private fun updateTimer() {
        val minutes = (timeLeftInMillis / 1000).toInt() / 60
        val seconds = (timeLeftInMillis / 1000).toInt() % 60
        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        val timerText = findViewById<TextView>(R.id.timerTextView)
        timerText.text = timeLeftFormatted
    }

    private fun startFullCandleTimer(startTime: Long) {
        val context = this
        countDownTimerFullCancles = object : CountDownTimer(fullCandleCountInMillis - (System.currentTimeMillis() - startTime), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                fullCanclesTimeLeft = millisUntilFinished
                val timerText = findViewById<TextView>(R.id.countDown)
                timerText.isVisible = true
                updateFullCandleTimer()
            }

            override fun onFinish() {
                countDownTimer?.cancel()
                mService?.makePhoneBuzz(context,1000)
                resetGame()
                val intent = Intent(context, HidersWonActivity::class.java)
                startActivity(intent)
            }
        }.start()
    }

    private fun stopFullCandleTimer(){
        countDownTimerFullCancles?.cancel()
        val timerText = findViewById<TextView>(R.id.countDown)
        timerText.isVisible = false
    }

    private fun updateFullCandleTimer(){
        val minutes = (fullCanclesTimeLeft / 1000).toInt() / 60
        val seconds = (fullCanclesTimeLeft / 1000).toInt() % 60
        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        val timerText = findViewById<TextView>(R.id.countDown)
        timerText.text = timeLeftFormatted

        // vibrate
        mService?.makePhoneBuzz(this,100)
    }

    private fun resetGame(){
        // Firebase realtime database
        val database = Firebase.database("https://nocturnal-lights-61563-default-rtdb.europe-west1.firebasedatabase.app")

        database.getReference("candle1_state").setValue(false)
        database.getReference("candle2_state").setValue(false)
        database.getReference("candle3_state").setValue(false)
        database.getReference("candle4_state").setValue(false)
        database.getReference("candle5_state").setValue(false)
        database.getReference("start time").setValue(0L)
        database.getReference("lightEvents").removeValue()
    }
}