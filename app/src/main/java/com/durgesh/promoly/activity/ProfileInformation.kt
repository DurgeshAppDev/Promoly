package com.durgesh.promoly.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.PreferenceManager
import com.durgesh.promoly.util.showToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileInformation : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
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
    private var base64ImageString: String? = null

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                try {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uriContent))
                    if (bitmap != null) {
                        ivEditProfileImage.setImageBitmap(bitmap)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                        val selectedImageBytes = byteArrayOutputStream.toByteArray()
                        base64ImageString = Base64.encodeToString(selectedImageBytes, Base64.DEFAULT)
                    }
                } catch (e: Exception) {
                    showToast("Failed to process cropped image")
                }
            }
        }
    }

    private val getImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val cropOptions = CropImageContractOptions(
                uri,
                CropImageOptions().apply {
                    guidelines = CropImageView.Guidelines.ON
                    aspectRatioX = 1
                    aspectRatioY = 1
                    fixAspectRatio = true
                    cropShape = CropImageView.CropShape.OVAL
                    activityTitle = "Crop & Confirm"
                    activityMenuIconColor = Color.WHITE
                    toolbarColor = Color.parseColor("#006C49")
                    toolbarTitleColor = Color.WHITE
                    toolbarBackButtonColor = Color.WHITE
                }
            )
            cropImage.launch(cropOptions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfileInfo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(this)

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
        btnChangeImage.setOnClickListener { getImageContract.launch("image/*") }
        findViewById<TextView>(R.id.tvChangePhoto).setOnClickListener { getImageContract.launch("image/*") }

        btnSaveChanges.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val emailText = etEmail.text.toString().trim()

            if (fullName.isEmpty()) {
                etFullName.error = "Full Name is required"
                return@setOnClickListener
            }
            if (emailText.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

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

                    uploadedImageUrl = document.getString("profileImageUrl")
                    if (!uploadedImageUrl.isNullOrEmpty()) {
                        if (uploadedImageUrl!!.startsWith("http")) {
                            Glide.with(this).load(uploadedImageUrl).into(ivEditProfileImage)
                        } else {
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

        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        val updatesMap = HashMap<String, Any>()
        updatesMap["name"] = name
        updatesMap["email"] = email
        updatesMap["bio"] = bio
        updatesMap["phone"] = phone
        updatesMap["updatedAt"] = FieldValue.serverTimestamp()

        val finalImageUrl = base64ImageString ?: uploadedImageUrl
        if (base64ImageString != null) {
            updatesMap["profileImageUrl"] = base64ImageString!!
        }

        showToast("Saving Changes...")
        btnSaveChanges.isEnabled = false

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update(updatesMap)
            .addOnSuccessListener {
                val currentFollowers = preferenceManager.getUserFollowers()
                preferenceManager.saveUserProfile(name, bio, finalImageUrl, currentFollowers)

                updateDenormalizedUserData(currentUserId, name, finalImageUrl) {
                    showToast("Profile Updated!")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
                btnSaveChanges.isEnabled = true
            }
    }

    private fun updateDenormalizedUserData(userId: String, newName: String, newImageUrl: String?, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()

        val taskUpdate = db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", userId)
            .get()
            .continueWithTask { task ->
                val batch = db.batch()
                for (doc in task.result.documents) {
                    val taskUpdates = hashMapOf<String, Any>("userName" to newName)
                    if (newImageUrl != null) taskUpdates["userProfileImage"] = newImageUrl
                    batch.update(doc.reference, taskUpdates)
                }
                batch.commit()
            }

        val collabSenderUpdate = db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("senderId", userId)
            .get()
            .continueWithTask { task ->
                val batch = db.batch()
                for (doc in task.result.documents) {
                    val collabUpdates = hashMapOf<String, Any>("senderName" to newName)
                    if (newImageUrl != null) collabUpdates["senderImage"] = newImageUrl
                    batch.update(doc.reference, collabUpdates)
                }
                batch.commit()
            }

        val collabReceiverUpdate = db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("receiverId", userId)
            .get()
            .continueWithTask { task ->
                val batch = db.batch()
                for (doc in task.result.documents) {
                    if (doc.contains("receiverName")) {
                        val collabUpdates = hashMapOf<String, Any>("receiverName" to newName)
                        if (newImageUrl != null) collabUpdates["receiverImage"] = newImageUrl
                        batch.update(doc.reference, collabUpdates)
                    }
                }
                batch.commit()
            }

        com.google.android.gms.tasks.Tasks.whenAllComplete(taskUpdate, collabSenderUpdate, collabReceiverUpdate)
            .addOnCompleteListener {
                onComplete()
            }
    }
}
