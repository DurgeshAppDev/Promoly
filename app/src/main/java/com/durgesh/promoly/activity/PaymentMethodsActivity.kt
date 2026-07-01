package com.durgesh.promoly.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.util.PreferenceManager

class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_methods)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPaymentMethods)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferenceManager = PreferenceManager(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        val ivProfileSmall = findViewById<ImageView>(R.id.ivProfileSmall)
        loadHeaderProfileImage(ivProfileSmall)

        // Placeholder for future logic
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Essential listeners for UI interactions if any were needed, otherwise left for future integration
    }

    private fun loadHeaderProfileImage(imageView: ImageView) {
        val imageUrl = preferenceManager.getUserImage()
        if (!imageUrl.isNullOrEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(this).load(imageUrl).placeholder(R.drawable.user).into(imageView)
            } else {
                try {
                    val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    imageView.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    imageView.setImageResource(R.drawable.user)
                }
            }
        }
    }
}
