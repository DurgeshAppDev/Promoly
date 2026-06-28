package com.durgesh.promoly.fragments

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
import com.durgesh.promoly.util.PreferenceManager

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var preferenceManager: PreferenceManager

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
        preferenceManager = PreferenceManager(requireContext())

        ivProfileLarge = view.findViewById(R.id.ivProfileLarge)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileBio = view.findViewById(R.id.tvProfileBio)
        tvProfileCollabsCount = view.findViewById(R.id.tvProfileCollabsCount)
        tvProfileTasksCount = view.findViewById(R.id.tvProfileTasksCount)
        tvProfileFollowersCount = view.findViewById(R.id.tvProfileFollowersCount)

        val personalInfo = view.findViewById<LinearLayout>(R.id.btnPersonalInfo)
        val notificationSettings = view.findViewById<LinearLayout>(R.id.btnNotificationSettings)
        val paymentMethods = view.findViewById<LinearLayout>(R.id.btnPaymentMethods)
        val logoutButton = view.findViewById<LinearLayout>(R.id.btnLogOut)

        loadCachedData()
        loadUserProfileData()

        personalInfo.setOnClickListener {
            val intent = Intent(requireContext(), ProfileInformation::class.java)
            startActivity(intent)
        }

        notificationSettings.setOnClickListener {
            showToast("Notification Settings Clicked")
        }

        paymentMethods.setOnClickListener {
            val intent = Intent(requireContext(), com.durgesh.promoly.activity.PaymentMethodsActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            Log.d("logout_button","Logout button was clicked")
            preferenceManager.clear()
            FcmUtils.deleteFcmToken()
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun loadCachedData() {
        tvProfileName.text = preferenceManager.getUserName()
        val bio = preferenceManager.getUserBio()
        tvProfileBio.text = if (bio.isEmpty()) "Digital Creator & Brand Strategist." else bio
        val followers = preferenceManager.getUserFollowers()
        tvProfileFollowersCount.text = if (followers >= 1000) "${followers / 1000}k" else followers.toString()
        tvProfileTasksCount.text = preferenceManager.getTasksCount().toString()
        tvProfileCollabsCount.text = preferenceManager.getCollabsCount().toString()

        val imageUrl = preferenceManager.getUserImage()
        if (!imageUrl.isNullOrEmpty()) {
            displayImage(imageUrl)
        }
    }

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "Name"
                    val bio = document.getString("bio") ?: ""
                    val imageUrl = document.getString("profileImageUrl")
                    val followers = document.getLong("followers") ?: 0L

                    tvProfileName.text = name
                    tvProfileBio.text = if (bio.isNullOrEmpty()) "Digital Creator & Brand Strategist." else bio
                    tvProfileFollowersCount.text = if (followers >= 1000) "${followers/1000}k" else followers.toString()

                    preferenceManager.saveUserProfile(name, bio, imageUrl, followers)

                    if (!imageUrl.isNullOrEmpty()) {
                        displayImage(imageUrl)
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

    private fun displayImage(imageUrl: String) {
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
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
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
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error decoding base64 image", e)
            }
        }
    }

    private fun loadStatCounts(userId: String) {
        // 1. Total Tasks count
        db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    val count = documents.size()
                    tvProfileTasksCount.text = count.toString()
                    preferenceManager.saveTasksCount(count)
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
                    preferenceManager.saveCollabsCount(count)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
    }
}
