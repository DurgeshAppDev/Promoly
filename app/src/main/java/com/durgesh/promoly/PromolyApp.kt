package com.durgesh.promoly

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class PromolyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
