package com.durgesh.promoly.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.durgesh.promoly.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Persistent Login Check: Skip login screen if user is already authenticated
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        loginUsername = findViewById(R.id.loginUsername)
        loginPassword = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginbtn)
        cbRemember = findViewById(R.id.cbRemember)
        loginWithGoogle = findViewById(R.id.loginWithGoogle)
        loginWithFacebook = findViewById(R.id.loginWithFacebook)
        textRegister = findViewById(R.id.textRegister)
        textForgotPass = findViewById(R.id.loginforgotpass)

        // Email & Password Login Button Click
        loginBtn.setOnClickListener {
            val email = loginUsername.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            if (email.isEmpty()) {
                loginUsername.error = "Enter Email Address"
                loginUsername.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                loginPassword.error = "Enter Password"
                loginPassword.requestFocus()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Authentication Failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Modern Google Login Handler
        loginWithGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Facebook Login
        loginWithFacebook.setOnClickListener {
            Toast.makeText(this, "Facebook Login Clicked", Toast.LENGTH_SHORT).show()
        }

        textForgotPass.setOnClickListener {
            startActivity(Intent(this, OtpActivity::class.java))
        }

        // Navigate to Register Activity
        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Google Sign-In with Credential Manager API
    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // Prompt selector even if single account remains
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Log.w(TAG, "Unexpected credential token type received")
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Credential Manager failure", e)
                Toast.makeText(this@LoginActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    if (user != null && isNewUser) {
                        // Onboard fresh Google sign-ins into Firestore
                        saveUserToDatabase(user.uid, user.displayName ?: "Google User", user.email ?: "")
                    } else if (user != null) {
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    Log.w(TAG, "Firebase context link failed", task.exception)
                    Toast.makeText(this, "Firebase Identity Link Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        // 1. Initialize Firestore Instance
        val db = FirebaseFirestore.getInstance()

        // 2. Map out your user payload
        val userMap = HashMap<String, Any>()
        userMap["uid"] = userId
        userMap["name"] = name
        userMap["email"] = email

        // 3. Save to a "Users" collection using the unique userId as the Document ID
        db.collection("Users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                // Fixed: Takes the authenticated user directly into the main app experience
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}