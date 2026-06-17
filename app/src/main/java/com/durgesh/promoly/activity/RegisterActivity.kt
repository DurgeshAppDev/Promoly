package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText

    private lateinit var registerBtn: Button

    private lateinit var googleRegister: TextView
    private lateinit var facebookRegister: TextView
    private lateinit var textLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        fullName = findViewById(R.id.registerfullname)
        email = findViewById(R.id.registerEmail)
        password = findViewById(R.id.registerPassword)
        confirmPassword = findViewById(R.id.registerConfirmPassword)

        registerBtn = findViewById(R.id.registerbtn)

        googleRegister = findViewById(R.id.RegisterWithGoogle)
        facebookRegister = findViewById(R.id.registerWithFacebook)
        textLogin = findViewById(R.id.textLogin)


        registerBtn.setOnClickListener {

            val name = fullName.text.toString()
            val emailText = email.text.toString()
            val pass = password.text.toString()
            val confirmPass = confirmPassword.text.toString()

            when {
                name.isEmpty() -> {
                    fullName.error = "Enter Full Name"
                    fullName.requestFocus()
                }

                emailText.isEmpty() -> {
                    email.error = "Enter Email"
                    email.requestFocus()
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches() -> {
                    email.error = "Enter Valid Email"
                    email.requestFocus()
                }

                pass.isEmpty() -> {
                    password.error = "Enter Password"
                    password.requestFocus()
                }

                pass.length < 6 -> {
                    password.error = "Password must be at least 6 characters"
                    password.requestFocus()
                }

                confirmPass.isEmpty() -> {
                    confirmPassword.error = "Confirm Password"
                    confirmPassword.requestFocus()
                }

                pass != confirmPass -> {
                    confirmPassword.error = "Passwords do not match"
                    confirmPassword.requestFocus()
                }

                else -> {
                    Toast.makeText(
                        this,
                        "Registration Successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Google Register
        googleRegister.setOnClickListener {
            Toast.makeText(
                this,
                "Google Registration Clicked",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Facebook Register
        facebookRegister.setOnClickListener {
            Toast.makeText(
                this,
                "Facebook Registration Clicked",
                Toast.LENGTH_SHORT
            ).show()
        }


        textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()

        }
    }
}