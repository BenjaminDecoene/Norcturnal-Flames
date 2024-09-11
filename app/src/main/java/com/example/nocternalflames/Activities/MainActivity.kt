package com.example.nocternalflames.Activities

import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nocternalflames.R
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import android.os.Handler


class MainActivity : AppCompatActivity(){
    // Local candleState
    private var candlesState = arrayOf(false,false,false,false,false)
    private var gameTime: Long = 600900 // 10 minutes
    private var timeLeftInMillis: Long = 601000 // 10 minutes
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

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
                // Count lit candles based on events
                for (snapshot in dataSnapshot.children) {
                    val newCount = count + snapshot.getValue().toString().toInt()
                    if (newCount < 0 || newCount > candles.size)
                        continue

                    count = newCount
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
            val itemsRef = database.getReference("lightEvents")

            val newItemRef = itemsRef.push()
            newItemRef.setValue(1)

            // Disable the button
            lightCandleButton.isEnabled = false

            // Re-enable the button after 10 seconds (10000 milliseconds)
            handler.postDelayed({ lightCandleButton.isEnabled = true }, 10000)
        }

        // Extinguish candle button
        val extinguishCandleButton = findViewById<Button>(R.id.extinguishCandleButton)
        extinguishCandleButton.setOnClickListener {
            val itemsRef = database.getReference("lightEvents")

            val newItemRef = itemsRef.push()
            newItemRef.setValue(-1)

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
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimer()
            }

            override fun onFinish() {
                // Timer finished
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