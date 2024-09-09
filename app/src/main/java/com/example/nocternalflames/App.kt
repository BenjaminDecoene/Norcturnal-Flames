package com.example.nocternalflames

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        println("Appp")
    }
}