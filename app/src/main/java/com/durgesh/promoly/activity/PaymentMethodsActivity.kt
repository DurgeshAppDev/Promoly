package com.durgesh.promoly.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R
import com.durgesh.promoly.util.PreferenceManager
import com.durgesh.promoly.util.showToast

class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_methods)

        preferenceManager = PreferenceManager(this)

        // Setup Back Button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Setup Profile Image in Header (Show only, no click action as requested)
        val ivProfileSmall = findViewById<ImageView>(R.id.ivProfileSmall)
        loadHeaderProfileImage(ivProfileSmall)

        // Click Listeners for Payment Methods (To-dos as requested)
        findViewById<ImageView>(R.id.ivOptions).setOnClickListener {
            showToast("Card options clicked")
        }

        findViewById<android.view.View>(R.id.btnPayPal).setOnClickListener {
            showToast("PayPal integration coming soon")
        }


        findViewById<android.view.View>(R.id.btnGooglePay).setOnClickListener {
            showToast("Google Pay integration coming soon")
        }

        findViewById<android.view.View>(R.id.btnAddCard).setOnClickListener {
            showToast("Add Card integration coming soon")
        }
    }

    private fun loadHeaderProfileImage(imageView: ImageView) {
        val imageUrl = preferenceManager.getUserImage()
        if (!imageUrl.isNullOrEmpty()) {
            if (imageUrl.startsWith("http")) {
                com.bumptech.glide.Glide.with(this).load(imageUrl).placeholder(R.drawable.user).into(imageView)
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
