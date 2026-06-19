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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Forgot_PsswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pssword)

        val forgotPass = findViewById<EditText>(R.id.forgotPass)
        val forgotConfirmPass = findViewById<EditText>(R.id.forgotconfirmPass)
        val changePass = findViewById<Button>(R.id.changePassword)

        auth = FirebaseAuth.getInstance()

        changePass.setOnClickListener {
            val pass = forgotPass.text.toString()
            val confirmPass = forgotConfirmPass.text.toString()

            if (pass.isEmpty()) {
                forgotPass.error = "Enter Password"
                forgotPass.requestFocus()
                return@setOnClickListener
            }
            if (pass.length < 8){
                forgotPass.error ="Password must contain atleast 8 characters"
                forgotPass.requestFocus()
                return@setOnClickListener
            }

            if (confirmPass.isEmpty()) {
                forgotConfirmPass.error = "Enter Confirm Password"
                forgotConfirmPass.requestFocus()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                forgotConfirmPass.error = "Passwords do not match"
                forgotConfirmPass.requestFocus()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.updatePassword(pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }
}