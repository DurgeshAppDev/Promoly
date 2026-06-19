package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var phoneInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var sendCodeBtn: Button
    private lateinit var verifyOtpBtn: Button
    private lateinit var resendOtpBtn: Button

    // Tracking variables for Firebase Phone Auth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val TAG = "PhoneAuthVerification"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        auth = FirebaseAuth.getInstance()

        // Bind layout views
        phoneInput = findViewById(R.id.otpPhone)
        otpInput = findViewById(R.id.otp) // Fixed: Matches your custom layout XML ID layout exactly
        sendCodeBtn = findViewById(R.id.changePasswordbtn)
        verifyOtpBtn = findViewById(R.id.verifyOtpbtn)
        resendOtpBtn = findViewById(R.id.resendOtpbtn)

        // 1. Send Verification SMS Request
        sendCodeBtn.setOnClickListener {
            val phoneNumber = phoneInput.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                phoneInput.error = "Enter your phone number"
                phoneInput.requestFocus()
                return@setOnClickListener
            }

            startPhoneNumberVerification(phoneNumber)
        }

        // 2. Verify the 6-Digit SMS Code written by user
        verifyOtpBtn.setOnClickListener {
            val code = otpInput.text.toString().trim()

            if (code.isEmpty()) {
                otpInput.error = "Enter the 6-digit OTP code"
                otpInput.requestFocus()
                return@setOnClickListener
            }

            if (storedVerificationId != null) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Please request an OTP first", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Resend Code Request
        resendOtpBtn.setOnClickListener {
            val phoneNumber = phoneInput.text.toString().trim()
            if (phoneNumber.isEmpty()) {
                phoneInput.error = "Enter phone number first"
                phoneInput.requestFocus()
                return@setOnClickListener
            }

            if (resendToken != null) {
                // Fixed: Explicitly passed using Kotlin's non-null assertion operator (!!) since we already checked it's not null
                resendVerificationCode(phoneNumber, resendToken!!)
            } else {
                Toast.makeText(this, "Cannot resend yet. Please wait for the code to be sent first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firebase Callback System
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "onVerificationFailed", e)
            Toast.makeText(this@OtpActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d(TAG, "onCodeSent:$verificationId")
            storedVerificationId = verificationId
            resendToken = token

            Toast.makeText(this@OtpActivity, "OTP Code sent to your device!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Fixed: Expects non-nullable ForceResendingToken payload parameter safely
    private fun resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "OTP Verified successfully!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, Forgot_PsswordActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid OTP code entered.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}