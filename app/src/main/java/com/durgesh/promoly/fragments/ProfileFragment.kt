package com.durgesh.promoly.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.graphics.BitmapFactory
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
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivProfileLarge: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileBio: TextView

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
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val bio = document.getString("bio") ?: ""
                    val imageUrl = document.getString("profileImageUrl")

                    tvProfileName.text = name
                    tvProfileBio.text = if (bio.isNullOrEmpty()) "Digital Creator & Brand Strategist." else bio

                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            // Legacy URL fallback
                            Glide.with(this).load(imageUrl).into(ivProfileLarge)
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
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    showToast("Failed to load profile: ${e.message}")
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
    }
}
