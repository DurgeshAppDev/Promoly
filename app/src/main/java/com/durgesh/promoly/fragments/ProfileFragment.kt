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
import com.durgesh.promoly.activity.NotificationSettingsActivity
import com.durgesh.promoly.activity.PaymentMethodsActivity
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
    private lateinit var tvProfileProfessionBadge: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvProfileCollabsCount: TextView
    private lateinit var tvProfileTasksCount: TextView
    private lateinit var tvProfileFollowersCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(requireContext())

        ivProfileLarge = view.findViewById(R.id.ivProfileLarge)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileProfessionBadge = view.findViewById(R.id.tvProfileProfessionBadge)
        tvProfileBio = view.findViewById(R.id.tvProfileBio)
        tvProfileCollabsCount = view.findViewById(R.id.tvProfileCollabsCount)
        tvProfileTasksCount = view.findViewById(R.id.tvProfileTasksCount)
        tvProfileFollowersCount = view.findViewById(R.id.tvProfileFollowersCount)

        loadCachedData()
        loadUserProfileData()

        view.findViewById<LinearLayout>(R.id.btnPersonalInfo).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileInformation::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnNotificationSettings).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnPaymentMethods).setOnClickListener {
            startActivity(Intent(requireContext(), PaymentMethodsActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnLogOut).setOnClickListener {
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
        val profession = preferenceManager.getUserProfession()
        tvProfileProfessionBadge.text = profession.ifEmpty { "Creator" }
        val bio = preferenceManager.getUserBio()
        tvProfileBio.text = bio.ifEmpty { "Digital Creator & Brand Strategist." }
        val followers = preferenceManager.getUserFollowers()
        tvProfileFollowersCount.text = if (followers >= 1000) "${followers / 1000}k" else followers.toString()
        tvProfileTasksCount.text = preferenceManager.getTasksCount().toString()
        tvProfileCollabsCount.text = preferenceManager.getCollabsCount().toString()

        preferenceManager.getUserImage()?.let { displayImage(it) }
    }

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "Name"
                    val profession = document.getString("profession") ?: ""
                    val bio = document.getString("bio") ?: ""
                    val imageUrl = document.getString("profileImageUrl")
                    val followers = document.getLong("followers") ?: 0L

                    tvProfileName.text = name
                    tvProfileProfessionBadge.text = profession.ifEmpty { "Creator" }
                    tvProfileBio.text = if (bio.isEmpty()) "Digital Creator & Brand Strategist." else bio
                    tvProfileFollowersCount.text = if (followers >= 1000) "${followers/1000}k" else followers.toString()

                    preferenceManager.saveUserProfile(name, profession, bio, imageUrl, followers)
                    imageUrl?.let { displayImage(it) }
                }
            }
            
        loadStatCounts(currentUserId)
    }

    private fun displayImage(imageUrl: String) {
        if (imageUrl.startsWith("http")) {
            Glide.with(this).load(imageUrl).into(ivProfileLarge)
        } else {
            try {
                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfileLarge.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                ivProfileLarge.setImageResource(R.drawable.user)
            }
        }
    }

    private fun loadStatCounts(userId: String) {
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
