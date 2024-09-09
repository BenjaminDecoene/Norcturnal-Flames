package com.example.nocternalflames.Activities

import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.nocternalflames.R

class MainActivity : AppCompatActivity(){
    private var candle1State = false
    private var candle2State = false
    private var candle3State = false
    private var candle4State = false
    private var candle5State = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        val candle1 = findViewById<ImageView>(R.id.candle1)
        candle1.setOnClickListener{
            if(candle1State){
                lightCandle(candle1, false)
                candle1State = false
            }
            else{
                lightCandle(candle1, true)
                candle1State = true
            }
        }

        val candle2 = findViewById<ImageView>(R.id.candle2)
        candle2.setOnClickListener{
            if(candle2State){
                lightCandle(candle2, false)
                candle2State = false
            }
            else{
                lightCandle(candle2, true)
                candle2State = true
            }
        }

        val candle3 = findViewById<ImageView>(R.id.candle3)
        candle3.setOnClickListener{
            if(candle3State){
                lightCandle(candle3, false)
                candle3State = false
            }
            else{
                lightCandle(candle3, true)
                candle3State = true
            }
        }

        val candle4 = findViewById<ImageView>(R.id.candle4)
        candle4.setOnClickListener{
            if(candle4State){
                lightCandle(candle4, false)
                candle4State = false
            }
            else{
                lightCandle(candle4, true)
                candle4State = true
            }
        }

        val candle5 = findViewById<ImageView>(R.id.candle5)
        candle5.setOnClickListener{
            if(candle5State){
                lightCandle(candle5, false)
                candle5State = false
            }
            else{
                lightCandle(candle5, true)
                candle5State = true
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