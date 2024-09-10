package com.example.nocternalflames.Activities

import android.content.ContentValues.TAG
import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.nocternalflames.R
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class MainActivity : AppCompatActivity(){
    // Local candleState
    private var candlesState = arrayOf(false,false,false,false,false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        // Firebase realtime database
        val database = Firebase.database("https://nocturnal-lights-61563-default-rtdb.europe-west1.firebasedatabase.app")

        // Init database refs
        val candlesdb = listOf(
            database.getReference("candle1_state"),
            database.getReference("candle2_state"),
            database.getReference("candle3_state"),
            database.getReference("candle4_state"),
            database.getReference("candle5_state")
            )

        // Init candle image views
        val candles = listOf(
            findViewById<ImageView>(R.id.candle1),
            findViewById<ImageView>(R.id.candle2),
            findViewById<ImageView>(R.id.candle3),
            findViewById<ImageView>(R.id.candle4),
            findViewById<ImageView>(R.id.candle5)
                )

        // Listen to candle values in the db
        candlesdb.forEachIndexed { index, candledb ->
            candledb.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    candlesState[index] = dataSnapshot.value as Boolean
                    lightCandle(candles[index], candlesState[index])
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })
        }

        // Toggle candle value in db
        candles.forEachIndexed { index, candle ->
            candle.setOnClickListener {
                candlesdb[index].setValue(candlesState[index].not())
            }
        }
    }

    fun lightCandle(candle:ImageView, light: Boolean){
        if (light)
            candle.setImageResource(R.drawable.candle_on)
        else
            candle.setImageResource(R.drawable.candle_off)
    }
}