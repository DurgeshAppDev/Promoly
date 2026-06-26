package com.durgesh.promoly.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.activity.LoginActivity
import com.durgesh.promoly.activity.ProfileInformation
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.FcmUtils
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    private lateinit var ivProfileLarge: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvProfileCollabsCount: TextView
    private lateinit var tvProfileTasksCount: TextView
    private lateinit var tvProfileFollowersCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivProfileLarge = view.findViewById(R.id.ivProfileLarge)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileBio = view.findViewById(R.id.tvProfileBio)
        tvProfileCollabsCount = view.findViewById(R.id.tvProfileCollabsCount)
        tvProfileTasksCount = view.findViewById(R.id.tvProfileTasksCount)
        tvProfileFollowersCount = view.findViewById(R.id.tvProfileFollowersCount)
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading profile...")
        progressDialog.setCancelable(false)

        val personalInfo = view.findViewById<LinearLayout>(R.id.btnPersonalInfo)
        val notificationSettings = view.findViewById<LinearLayout>(R.id.btnNotificationSettings)
        val paymentMethods = view.findViewById<LinearLayout>(R.id.btnPaymentMethods)
        val logoutButton = view.findViewById<LinearLayout>(R.id.btnLogOut)

        loadUserProfileData()

        personalInfo.setOnClickListener {
            val intent = Intent(requireContext(), ProfileInformation::class.java)
            startActivity(intent)
        }

        notificationSettings.setOnClickListener {
            showToast("Notification Settings Clicked")
        }

        paymentMethods.setOnClickListener {
            showToast("Payment Methods Clicked")
        }

        logoutButton.setOnClickListener {
            Log.d("logout_button","Logout button was clicked")
            FcmUtils.deleteFcmToken()
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun loadUserProfileData() {
        progressDialog.show()
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val bio = document.getString("bio") ?: ""
                    val imageUrl = document.getString("profileImageUrl")
                    val followers = document.getLong("followers") ?: 0L

                    tvProfileName.text = name
                    tvProfileBio.text = if (bio.isNullOrEmpty()) "Digital Creator & Brand Strategist." else bio
                    tvProfileFollowersCount.text = if (followers >= 1000) "${followers/1000}k" else followers.toString()

                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            // Legacy URL fallback
                            Glide.with(this)
                                .load(imageUrl)
                                .listener(object : RequestListener<Drawable> {


                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: com.bumptech.glide.request.target.Target<Drawable?>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        progressDialog.dismiss()
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        progressDialog.dismiss()
                                        return false
                                    }
                                })
                                .into(ivProfileLarge)
                        } else {
                            // Decode and load Base64 string
                            try {
                                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivProfileLarge.setImageBitmap(decodedImage)
                                progressDialog.dismiss()
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Error decoding base64 image", e)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    showToast("Failed to load profile: ${e.message}")
                }
            }
            
        // Load counts
        loadStatCounts(currentUserId)
    }

    private fun loadStatCounts(userId: String) {
        // 1. Total Tasks count
        db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    tvProfileTasksCount.text = documents.size().toString()
                }
            }

        // 2. Total Completed Collabs count
        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("status", "Completed")
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    var count = 0
                    for (doc in documents) {
                        if (doc.getString("senderId") == userId || doc.getString("receiverId") == userId) {
                            count++
                        }
                    }
                    tvProfileCollabsCount.text = count.toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
    }
}
