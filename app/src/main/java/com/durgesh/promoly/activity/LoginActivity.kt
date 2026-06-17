package com.durgesh.promoly.activity


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

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            // Initialize Views
            loginUsername = findViewById(R.id.loginUsername)
            loginPassword = findViewById(R.id.loginPassword)
            loginBtn = findViewById(R.id.loginbtn)
            cbRemember = findViewById(R.id.cbRemember)
            loginWithGoogle = findViewById(R.id.loginWithGoogle)
            loginWithFacebook = findViewById(R.id.loginWithFacebook)
            textRegister = findViewById(R.id.textRegister)

            // Login Button Click
            loginBtn.setOnClickListener {

                val username = loginUsername.text.toString().trim()
                val password = loginPassword.text.toString().trim()

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

                // TODO: Firebase Login Code Here
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

            // Register
            textRegister.setOnClickListener {
                Toast.makeText(
                    this,
                    "Open Register Screen",
                    Toast.LENGTH_SHORT
                ).show()

                // startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }