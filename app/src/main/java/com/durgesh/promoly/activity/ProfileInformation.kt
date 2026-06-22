package com.durgesh.promoly.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide // Optional: Highly recommended for loading URLs into ImageViews
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileInformation : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Elements
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBio: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnCancel: TextView
    private lateinit var btnBackInfo: ImageView
    private lateinit var ivEditProfileImage: ImageView
    private lateinit var btnChangeImage: MaterialCardView

    private var uploadedImageUrl: String? = null

    private var selectedImageUri: Uri? = null
    private var base64ImageString: String? = null
    private var selectedImageBytes: ByteArray? = null
    // Photo Picker Contract to grab an image from the gallery
    private val getImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            
            try {
                // Downsample and process the image efficiently
                val inputStream = contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                // Calculate scale factor to avoid massive bitmaps
                var scale = 1
                while (options.outWidth / scale / 2 >= 800 && options.outHeight / scale / 2 >= 800) {
                    scale *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = scale
                }
                val finalInputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(finalInputStream, null, decodeOptions)
                finalInputStream?.close()

                if (bitmap != null) {
                    ivEditProfileImage.setImageBitmap(bitmap)
                    
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                    selectedImageBytes = byteArrayOutputStream.toByteArray()
                    base64ImageString = Base64.encodeToString(selectedImageBytes, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                showToast("Failed to process image: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_information)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind Views
        btnBackInfo = findViewById(R.id.btnBackInfo)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etBio = findViewById(R.id.etBio)
        etPhone = findViewById(R.id.etPhone)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnCancel = findViewById(R.id.btnCancel)
        ivEditProfileImage = findViewById(R.id.ivEditProfileImage)
        btnChangeImage = findViewById(R.id.btnChangeImage)

        loadUserProfileData()

        btnBackInfo.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        // Trigger gallery picker when clicking the edit image buttons
        btnChangeImage.setOnClickListener { getImageContract.launch("image/*") }
        findViewById<TextView>(R.id.tvChangePhoto).setOnClickListener { getImageContract.launch("image/*") }

        // Update this inside your onCreate method in ProfileInformation.kt
        btnSaveChanges.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val emailText = etEmail.text.toString().trim()

            // 1. Mandatory Text Validations
            if (fullName.isEmpty()) {
                etFullName.error = "Full Name is required"
                return@setOnClickListener
            }
            if (emailText.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            // Direct save to Firestore (No-cost friendly)
            saveProfileInformation()
        }
    }

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etFullName.setText(document.getString("name"))
                    etEmail.setText(document.getString("email"))
                    etBio.setText(document.getString("bio") ?: "")
                    etPhone.setText(document.getString("phone") ?: "")

                    // Retrieve existing profile image link or base64
                    uploadedImageUrl = document.getString("profileImageUrl")
                    if (!uploadedImageUrl.isNullOrEmpty()) {
                        if (uploadedImageUrl!!.startsWith("http")) {
                            // Legacy URL fallback
                            Glide.with(this).load(uploadedImageUrl).into(ivEditProfileImage)
                        } else {
                            // Decode and load Base64 string
                            try {
                                val imageBytes = Base64.decode(uploadedImageUrl, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivEditProfileImage.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
    }

    private fun saveProfileInformation() {
        val currentUserId = auth.currentUser?.uid ?: return

        val updatesMap = HashMap<String, Any>()
        updatesMap["name"] = etFullName.text.toString().trim()
        updatesMap["email"] = etEmail.text.toString().trim()
        updatesMap["bio"] = etBio.text.toString().trim()
        updatesMap["phone"] = etPhone.text.toString().trim()
        updatesMap["updatedAt"] = FieldValue.serverTimestamp()

        // If a new image was selected and encoded, save its Base64 string
        if (base64ImageString != null) {
            updatesMap["profileImageUrl"] = base64ImageString!!
        }

        showToast("Saving Profile Changes...")

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update(updatesMap)
            .addOnSuccessListener {
                showToast("Profile Saved Successfully!")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error saving data: ${e.message}")
            }
    }
}
