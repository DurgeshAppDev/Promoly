package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.durgesh.promoly.R

class Forgot_PsswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pssword)

        val forgotPass = findViewById<EditText>(R.id.forgotPass)
        val forgotConfirmPass = findViewById<EditText>(R.id.forgotconfirmPass)
        val changeaPass = findViewById<Button>(R.id.changePassword)

        changeaPass.setOnClickListener {
            val pass = forgotPass.text.toString()
            val confirmPass = forgotConfirmPass.text.toString()

            if (pass.isEmpty()) {
                forgotPass.error = "Enter Password"
                forgotPass.requestFocus()
                return@setOnClickListener
            }

            if (confirmPass.isEmpty()) {
                forgotConfirmPass.error = "Enter Confirm Password"
                forgotConfirmPass.requestFocus()
                return@setOnClickListener
            }

            Toast.makeText(this,"Password cahnged Successfully ", Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}