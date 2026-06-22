package com.durgesh.promoly.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterCollabRequest
import com.durgesh.promoly.adapter.AdapterTasks
import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.model.ModelTasks
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvGreeting: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

         ivProfileImage = view.findViewById(R.id.ivProfileImageofHome)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        val searchEditText = view.findViewById<EditText>(R.id.etSearchHome)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearchHome)
        val rvCollabRequests = view.findViewById<RecyclerView>(R.id.rvCollabRequests)
        val rvTodaysTasks = view.findViewById<RecyclerView>(R.id.rvTodaysTasks)

        ivProfileImage.setOnClickListener {
            val intent = Intent(requireContext(), ProfileFragment::class.java)
            startActivity(intent)
        }

        // Dummy data for Tasks
        val tasksList = listOf(
            ModelTasks("1", R.drawable.ic_tasks, "Post Review", "Review for XYZ Brand on Instagram", "Pending"),
            ModelTasks("2", R.drawable.ic_tasks, "Video Editing", "Edit the collab video with Alex", "In Progress"),
            ModelTasks("3", R.drawable.ic_tasks, "Campaign Pitch", "Submit pitch for New Winter Collection", "Completed"),
            ModelTasks("4", R.drawable.ic_tasks, "Client Call", "Meeting with Brand Manager at 5 PM", "Scheduled"),
            ModelTasks("5", R.drawable.ic_tasks, "Content Calendar", "Update next month's post schedule", "Pending")
        )

        rvTodaysTasks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvTodaysTasks.adapter = AdapterTasks(tasksList)

        // Dummy data for Collab Requests
        val collabList = listOf(
            ModelCollabRequest("1", R.drawable.profile_image, "Sarah (Designer)", "Wants to collaborate on UI Project"),
            ModelCollabRequest("2", R.drawable.profile_image, "Alex (Marketing)", "Brand Campaign Pitch"),
            ModelCollabRequest("3", R.drawable.profile_image, "John (Dev)", "E-commerce App Collab"),
            ModelCollabRequest("4", R.drawable.profile_image, "Emma (Writer)", "Blog Content Strategy"),
            ModelCollabRequest("5", R.drawable.profile_image, "Mike (Photo)", "Product Photoshoot Request")
        )

        rvCollabRequests.layoutManager = LinearLayoutManager(requireContext())
        rvCollabRequests.adapter = AdapterCollabRequest(collabList)


        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            if (searchText.isNotEmpty()) {
                FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS)
                    .whereGreaterThanOrEqualTo("username", searchText)
                    .whereLessThanOrEqualTo("username", searchText + "\uf8ff")
                    .get()
                    .addOnSuccessListener { documents ->
                        // Update your adapter with documents.toObjects(User::class.java)
                    }
                    .addOnFailureListener { e ->
                        showToast("Error: ${e.message}")
                    }
            }
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
                    val imageUrl = document.getString("profileImageUrl")

                    tvGreeting.text = "Hello ${name}"

                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            // Legacy URL fallback
                            Glide.with(this).load(imageUrl).into(ivProfileImage)
                        } else {
                            // Decode and load Base64 string
                            try {
                                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivProfileImage.setImageBitmap(decodedImage)
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