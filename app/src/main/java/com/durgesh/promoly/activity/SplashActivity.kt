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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix for "Internal error in Cloud Firestore": Use memory-only cache to avoid corrupted disk persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            // Check if user is already logged in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 3000)

    }
}
