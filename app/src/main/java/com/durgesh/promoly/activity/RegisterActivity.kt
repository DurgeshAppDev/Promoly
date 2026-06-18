package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

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

        auth = FirebaseAuth.getInstance()

        fullName = findViewById(R.id.registerfullname)
        email = findViewById(R.id.registerEmail)
        password = findViewById(R.id.registerPassword)
        confirmPassword = findViewById(R.id.registerConfirmPassword)

        registerBtn = findViewById(R.id.registerbtn)

        googleRegister = findViewById(R.id.RegisterWithGoogle)
        facebookRegister = findViewById(R.id.registerWithFacebook)
        textLogin = findViewById(R.id.textLogin)

        registerBtn.setOnClickListener {

            val name = fullName.text.toString().trim()
            val emailText = email.text.toString().trim()
            val pass = password.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            // Validation
            if (name.isEmpty()) {
                fullName.error = "Enter Full Name"
                return@setOnClickListener
            }

            if (emailText.isEmpty()) {
                email.error = "Enter Email"
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                password.error = "Enter Password"
                return@setOnClickListener
            }

            if (pass.length < 6) {
                password.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerUser(name, emailText, pass)
        }

        googleRegister.setOnClickListener {
            Toast.makeText(this, "Google Registration Clicked", Toast.LENGTH_SHORT).show()
        }

        facebookRegister.setOnClickListener {
            Toast.makeText(this, "Facebook Registration Clicked", Toast.LENGTH_SHORT).show()
        }

        textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(name: String, email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser!!.uid

                    val userMap = HashMap<String, Any>()
                    userMap["uid"] = userId
                    userMap["name"] = name
                    userMap["email"] = email

                    FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(userId)
                        .setValue(userMap)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Registration Successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this,
                                    LoginActivity::class.java
                                )
                            )
                            finish()
                        }

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}