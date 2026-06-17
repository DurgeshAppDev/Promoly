package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.durgesh.promoly.R
import kotlin.jvm.java


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val logo = findViewById<ImageView>(R.id.logo)
        val appName = findViewById<TextView>(R.id.tvAppName)
        val tagline = findViewById<TextView>(R.id.tvTagline)

        // Initial state
        logo.alpha = 0f
        appName.alpha = 0f
        tagline.alpha = 0f

        // Fade-in animations
        logo.animate()
            .alpha(1f)
            .setDuration(800)
            .start()

        appName.animate()
            .alpha(1f)
            .setDuration(1500)
            .start()

        tagline.animate()
            .alpha(1f)
            .setDuration(1500)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)

    }
}