package com.durgesh.promoly.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.FcmUtils
import com.durgesh.promoly.util.showToast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLogin)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        loginUsername = findViewById(R.id.loginUsername)
        loginPassword = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginbtn)

        loginBtn.setOnClickListener {
            val email = loginUsername.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            if (email.isEmpty()) {
                loginUsername.error = "Enter Email Address"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                loginPassword.error = "Enter Password"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            checkAndInitializeUserData(user.uid, email)
                        }
                    } else {
                        showToast("Failed: ${task.exception?.message}")
                    }
                }
        }

        findViewById<TextView>(R.id.loginWithGoogle).setOnClickListener {
            signInWithGoogle()
        }

        findViewById<TextView>(R.id.loginforgotpass).setOnClickListener {
            startActivity(Intent(this, OtpActivity::class.java))
        }

        findViewById<TextView>(R.id.textRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
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
                }
            } catch (e: GetCredentialException) {
                showToast("Google Sign-In failed")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkAndInitializeUserData(user.uid, user.email ?: "", user.displayName ?: "Name")
                        FcmUtils.updateFcmToken()
                    }
                } else {
                    showToast("Authentication Failed.")
                }
            }
    }

    private fun checkAndInitializeUserData(userId: String, email: String, providedName: String? = null) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection(Constants.COLLECTION_USERS).document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                showToast("Welcome back!")
                FcmUtils.updateFcmToken()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                val userMap = HashMap<String, Any>()
                userMap["uid"] = userId
                userMap["name"] = providedName ?: email.substringBefore("@")
                userMap["email"] = email
                userMap["bio"] = ""
                userMap["phone"] = ""
                userMap["profileImageUrl"] = ""
                userMap["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

                userDocRef.set(userMap)
                    .addOnSuccessListener {
                        showToast("Login Successful!")
                        FcmUtils.updateFcmToken()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showToast("Initialization error: ${e.message}")
                    }
            }
        }.addOnFailureListener { e ->
            showToast("Failed to verify account")
        }
    }
}
