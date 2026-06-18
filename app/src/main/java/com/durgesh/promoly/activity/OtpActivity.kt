package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.durgesh.promoly.R

class OtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)
        val email = findViewById<EditText>(R.id.otpEmail)
        val otp = findViewById<EditText>(R.id.opt)
        val requestOtp = findViewById<Button>(R.id.changePasswordbtn)
        val verifyOtp = findViewById<Button>(R.id.verifyOtpbtn)
        val resendOtp = findViewById<Button>(R.id.resendOtpbtn)

        resendOtp.setOnClickListener {
            startActivity(Intent(this, Forgot_PsswordActivity::class.java))
        }

    }
}