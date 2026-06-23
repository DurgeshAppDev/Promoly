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
    private lateinit var tvNewCollabs: TextView
    private lateinit var tvActiveTasksCount: TextView
    private lateinit var tvActiveCollabsCount: TextView
    private lateinit var tvCompletedCollabsCount: TextView
    
    private lateinit var collabAdapter: AdapterCollabRequest
    private val collabList = mutableListOf<ModelCollabRequest>()

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
        tvNewCollabs = view.findViewById(R.id.tvNewCollabs)
        tvActiveTasksCount = view.findViewById(R.id.tvActiveTasksCount)
        tvActiveCollabsCount = view.findViewById(R.id.tvActiveCollabsCount)
        tvCompletedCollabsCount = view.findViewById(R.id.tvCompletedCollabsCount)
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

        // Setup Collab Requests RecyclerView
        rvCollabRequests.layoutManager = LinearLayoutManager(requireContext())
        collabAdapter = AdapterCollabRequest(collabList) {
            // Callback when a request is accepted or declined
            loadCollabRequests()
        }
        rvCollabRequests.adapter = collabAdapter


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

    private fun loadCollabRequests() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    collabList.clear()
                    for (doc in documents) {
                        val request = ModelCollabRequest(
                            id = doc.id,
                            coProfileImg = doc.getString("senderImage") ?: "",
                            coName = doc.getString("senderName") ?: "User",
                            coDescription = "Collab on: ${doc.getString("taskTitle") ?: "Project"}",
                            senderId = doc.getString("senderId") ?: "",
                            taskId = doc.getString("taskId") ?: "",
                            status = doc.getString("status") ?: "Pending"
                        )
                        collabList.add(request)
                    }
                    collabAdapter.notifyDataSetChanged()
                    tvNewCollabs.text = "${collabList.size} new"
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("HomeFragment", "Error loading collab requests", e)
                }
            }
    }

    private fun loadStatCounts() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. My Tasks count
        db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    tvActiveTasksCount.text = documents.size().toString()
                }
            }

        // 2. Active Collabs (Accepted)
        // We need to check where status is Accepted and user is either sender or receiver
        // Since Firestore doesn't support OR on different fields easily, we can query by status and filter locally
        // OR we can run two queries. For simplicity in a small app, we can just fetch all accepted for the user if we have a way.
        // Actually, we can just query where status == "Accepted" and then filter currentUserId in senderId or receiverId.
        
        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("status", "Accepted")
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    var count = 0
                    for (doc in documents) {
                        if (doc.getString("senderId") == currentUserId || doc.getString("receiverId") == currentUserId) {
                            count++
                        }
                    }
                    tvActiveCollabsCount.text = count.toString()
                }
            }

        // 3. Completed Collabs (Growth)
        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("status", "Completed")
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    var count = 0
                    for (doc in documents) {
                        if (doc.getString("senderId") == currentUserId || doc.getString("receiverId") == currentUserId) {
                            count++
                        }
                    }
                    tvCompletedCollabsCount.text = count.toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
        loadCollabRequests()
        loadStatCounts()
    }
}