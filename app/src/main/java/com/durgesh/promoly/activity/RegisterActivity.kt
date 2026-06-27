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
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRegister)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        fullName = findViewById(R.id.registerfullname)
        email = findViewById(R.id.registerEmail)
        password = findViewById(R.id.registerPassword)
        confirmPassword = findViewById(R.id.registerConfirmPassword)

        registerBtn = findViewById(R.id.registerbtn)

        googleRegister = findViewById(R.id.RegisterWithGoogle)
        facebookRegister = findViewById(R.id.registerWithFacebook)
        textLogin = findViewById(R.id.textLogin)

        auth = FirebaseAuth.getInstance()

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
            if (pass.length < 8) {
                password.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }
            if (pass != confirmPass) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerUser(name, emailText, pass)
        }

        googleRegister.setOnClickListener {
            signInWithGoogle()
        }

        facebookRegister.setOnClickListener {
            showToast("Facebook Registration Clicked")
        }

        textLogin.setOnClickListener {
            // Adjust package target if your LoginActivity is in the same .activity package
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }


    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    saveUserToDatabase(userId, name, email)
                } else {
                    showToast(task.exception?.message ?: "Unknown error", Toast.LENGTH_LONG)
                }
            }
    }

    // Google sign in setup
    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        // Setup the modern Google ID request option
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // Shows all Google accounts on the device
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Credential Manager executes using Coroutines asynchronously
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(this@RegisterActivity, request)
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                    // Pass the verified token directly to Firebase
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Log.w(TAG, "Unexpected credential type returned")
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Sign-In failed via Credential Manager", e)
                showToast("Sign-In failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        // Saves the newly logged-in user details to Cloud Firestore
                        saveUserToDatabase(user.uid, user.displayName ?: "Name", user.email ?: "")
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showToast("Authentication Failed.")
                }
            }
    }


    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val db = FirebaseFirestore.getInstance()

        // Match the complete structural payload you expect to update later
        val userMap = HashMap<String, Any>()
        userMap["uid"] = userId
        userMap["name"] = name
        userMap["email"] = email
        userMap["bio"] = ""
        userMap["phone"] = ""
        userMap["profileImageUrl"] = ""
        userMap["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

        db.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showToast("Registration Successful!")

                // FIX: Navigate directly to HomeActivity because they are already logged in!
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ERR", "Error setting document", e)
                showToast("Firestore error: ${e.message}")
            }
    }
}