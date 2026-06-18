package com.durgesh.promoly.activity


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R

class LoginActivity : AppCompatActivity() {

        private lateinit var loginUsername: EditText
        private lateinit var loginPassword: EditText
        private lateinit var loginBtn: Button
        private lateinit var cbRemember: CheckBox
        private lateinit var loginWithGoogle: TextView
        private lateinit var loginWithFacebook: TextView
        private lateinit var textRegister: TextView
        private lateinit var textForgotPass: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            loginUsername = findViewById(R.id.loginUsername)
            loginPassword = findViewById(R.id.loginPassword)
            loginBtn = findViewById(R.id.loginbtn)
            cbRemember = findViewById(R.id.cbRemember)
            loginWithGoogle = findViewById(R.id.loginWithGoogle)
            loginWithFacebook = findViewById(R.id.loginWithFacebook)
            textRegister = findViewById(R.id.textRegister)
            textForgotPass = findViewById(R.id.loginforgotpass)

            // Login Button Click
            loginBtn.setOnClickListener {

                val username = loginUsername.text.toString()
                val password = loginPassword.text.toString()

                if (username.isEmpty()) {
                    loginUsername.error = "Enter Username"
                    loginUsername.requestFocus()
                    return@setOnClickListener
                }

                if (password.isEmpty()) {
                    loginPassword.error = "Enter Password"
                    loginPassword.requestFocus()
                    return@setOnClickListener
                }

                Toast.makeText(
                    this,
                    "Login Successful",
                    Toast.LENGTH_SHORT
                ).show()

            }

            // Google Login
            loginWithGoogle.setOnClickListener {
                Toast.makeText(
                    this,
                    "Google Login Clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Facebook Login
            loginWithFacebook.setOnClickListener {
                Toast.makeText(
                    this,
                    "Facebook Login Clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            textForgotPass.setOnClickListener {
                startActivity(Intent(this, OtpActivity::class.java))
            }

            // Register
            textRegister.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }