package com.example.nocternalflames.Activities

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.example.nocternalflames.Foreground
import com.example.nocternalflames.LightEvent
import com.example.nocternalflames.R
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.Random
import kotlin.math.min


class MainActivity : AppCompatActivity(){
    // Local candleState
    private var gameTime: Long = 1200900 // 20 minutes
    private var timeLeftInMillis: Long = 1201000 // 20 minutes
    private var countDownTimer: CountDownTimer? = null
    private var handler = Handler()
    private var fullCandleCountInMillis: Long = 30000
    private var fullCanclesTimeLeft: Long = 30000
    private var countDownTimerFullCancles: CountDownTimer? = null
    private var candleCount =  0
    private var maxCandles = 7
    private var minCandles = 5
    private lateinit var fadeAnimator : ObjectAnimator

    // For sound
    private lateinit var soundPool: SoundPool
    private var soundIds: MutableList<Int> = mutableListOf()
    private val soundHandler: Handler? = null
    private var random: Random? = null


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

        // Setup soundPool

        // Initialize SoundPool with proper audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()


        // Load the sound file
        soundIds.add(soundPool.load(this, R.raw.footsteps_stop, 1))
        soundIds.add(soundPool.load(this, R.raw.organ, 1))
        soundIds.add(soundPool.load(this, R.raw.ghost, 1))
        soundIds.add(soundPool.load(this, R.raw.tension, 1))
        soundIds.add(soundPool.load(this, R.raw.behind_you, 1))
        soundIds.add(soundPool.load(this, R.raw.whisper_trail, 1))
        soundIds.add(soundPool.load(this, R.raw.whisper5, 1))
        soundIds.add(soundPool.load(this, R.raw.banging_noise, 1))
        soundIds.add(soundPool.load(this, R.raw.horror, 1))
        soundIds.add(soundPool.load(this, R.raw.horror_realisation, 1))
        soundIds.add(soundPool.load(this, R.raw.horror_sfx_3, 1))
        soundIds.add(soundPool.load(this, R.raw.paranormal, 1))
        soundIds.add(soundPool.load(this, R.raw.female_horror, 1))
        soundIds.add(soundPool.load(this, R.raw.stairs, 1))


        // Initialize Handler and Random
        handler = Handler(Looper.getMainLooper())
        random = Random()

        scheduleRandomSound()

        //------------------------------------------------------------------------

        // Setup the fadeAnimator
        val imageView = findViewById<ImageView>(R.id.full_glow)

        // Create an ObjectAnimator for the alpha property (fade in and out)
        val fadeAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
        fadeAnimator.duration = 1000 // 2 seconds for a fade in or fade out
        fadeAnimator.repeatCount = ObjectAnimator.INFINITE
        fadeAnimator.repeatMode = ObjectAnimator.REVERSE

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
                    if (newCount < 0 || newCount > maxCandles)
                        continue

                    count = newCount

                    // save the timestamp of when the candles go from 4 to 5
                    if(count == minCandles && lastCount == minCandles - 1){
                        countdownStartTimer = lightEvent.timestamp
                    }
                    lastCount = count
                }
                candleCount = count

                updateCandleGlow(candleCount)

                // Start countdown if there are 5 or more candles
                if(candleCount >= minCandles) {
                    startFullCandleTimer(countdownStartTimer)
                    // Start the animation
                    fadeAnimator.start()
                    println("Counting down")
                }else{
                    println(candleCount)
                    stopFullCandleTimer()
                    // Cancel the animation
                    fadeAnimator.cancel()

                    // Animate the alpha to 0 (fully transparent) after canceling
                    val fadeOutAnimator = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 0f)
                    fadeOutAnimator.duration = 500 // Duration of the fade out (adjust as needed)

                    fadeOutAnimator.start()
                }

                // Extinguish all candles
                candles.forEach {
                    lightCandle(it, false)
                }

                // Light candles based on count
                for (i in 0..min(count - 1, minCandles - 1)){
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

    override fun onDestroy() {
        super.onDestroy()
        if (soundPool != null) {
            soundPool.release()
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null)
        }
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

        if (countDownTimerFullCancles != null)
            return

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
        println("stopFullCandleTimer")
        println(countDownTimerFullCancles)
        countDownTimerFullCancles?.cancel()
        countDownTimerFullCancles = null
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

    private fun updateCandleGlow(candles: Int){
        val imageFadeOut = findViewById<ImageView>(R.id.candle_glow_past)

        // Setup the fadeAnimator
        val imageFadeIn = findViewById<ImageView>(R.id.candle_glow)

        imageFadeOut.setImageDrawable(imageFadeIn.drawable)
        imageFadeOut.alpha = 1f

        // Create an ObjectAnimator for the alpha property (fade in and out)
        val fadeoutAnimator = ObjectAnimator.ofFloat(imageFadeOut, "alpha", 1f, 0f)
        fadeoutAnimator.duration = 1000 // 1 seconds for a fade in or fade out

        fadeoutAnimator.start()

        imageFadeIn.alpha = 0f

        if(candles == 0){
            imageFadeIn.setImageResource(R.drawable.empty)
        }else if(candles == 1){
            imageFadeIn.setImageResource(R.drawable.candle_1)
        }else if(candles == 2){
            imageFadeIn.setImageResource(R.drawable.candles_2)
        }else if(candles == 3){
            imageFadeIn.setImageResource(R.drawable.candles_3)
        }else if(candles == 4){
            imageFadeIn.setImageResource(R.drawable.candles_4)
        }else if(candles == 5){
            imageFadeIn.setImageResource(R.drawable.empty)
        }

        // Create an ObjectAnimator for the alpha property (fade in and out)
        val fadeAnimator = ObjectAnimator.ofFloat(imageFadeIn, "alpha", 0f, 1f)
        fadeAnimator.duration = 1000 // 1 seconds for a fade in or fade out

        fadeAnimator.start()
    }

    // Schedule sound to play at random intervals around 10 minutes
    private fun scheduleRandomSound() {
        // Generate a random interval between 8 to 12 minutes (in milliseconds)
        val minInterval = 8 * 60 * 1000 // 8 minutes in milliseconds
        val maxInterval = 15 * 60 * 1000 // 12 minutes in milliseconds
        val randomDelay = random!!.nextInt(maxInterval - minInterval) + minInterval
        val randomId = soundIds[random!!.nextInt(soundIds.size)]

        // Post a delayed task to play the sound
        handler.postDelayed({ // Play the sound
            if (countDownTimer != null) {
                //------------------------------------------------------------------------
                // Volume op max zetten

                // Get the AudioManager system service
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)

                mService?.playSoundEffect(soundPool, randomId)
            }
            // Reschedule the next random sound
            scheduleRandomSound()
        }, randomDelay.toLong())
    }
}