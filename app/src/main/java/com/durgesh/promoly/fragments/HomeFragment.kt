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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.activity.SearchUserActivity
import com.durgesh.promoly.adapter.AdapterCollabRequest
import com.durgesh.promoly.adapter.AdapterTasks
import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.model.ModelTasks
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.DataRepository
import com.durgesh.promoly.util.PreferenceManager
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvGreeting: TextView
    private lateinit var tvNewCollabs: TextView
    private lateinit var tvActiveTasksCount: TextView
    private lateinit var tvActiveCollabsCount: TextView
    private lateinit var tvCompletedCollabsCount: TextView
    private lateinit var llEmptyCollabs: View
    private lateinit var pbHome: android.widget.ProgressBar
    private lateinit var rvCollabRequests: RecyclerView
    private lateinit var rvTodaysTasks: RecyclerView
    
    private lateinit var collabAdapter: AdapterCollabRequest
    private val collabList = mutableListOf<ModelCollabRequest>()
    
    private lateinit var taskAdapter: AdapterTasks
    private val taskList = mutableListOf<ModelTasks>()

    private var progressDialog: AlertDialog? = null
    private var lastCollabDoc: DocumentSnapshot? = null
    private var isCollabLoading = false
    private var hasMoreCollabs = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(requireContext())

        ivProfileImage = view.findViewById(R.id.ivProfileImageofHome)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvNewCollabs = view.findViewById(R.id.tvNewCollabs)
        tvActiveTasksCount = view.findViewById(R.id.tvActiveTasksCount)
        tvActiveCollabsCount = view.findViewById(R.id.tvActiveCollabsCount)
        tvCompletedCollabsCount = view.findViewById(R.id.tvCompletedCollabsCount)
        llEmptyCollabs = view.findViewById(R.id.llEmptyCollabs)
        pbHome = view.findViewById(R.id.pbHome)
        rvCollabRequests = view.findViewById(R.id.rvCollabRequests)
        rvTodaysTasks = view.findViewById(R.id.rvTodaysTasks)
        
        val cvSearch = view.findViewById<View>(R.id.cvSearchHome)

        ivProfileImage.setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav?.selectedItemId = R.id.navigation_profile
        }

        cvSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchUserActivity::class.java))
        }

        rvTodaysTasks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        taskAdapter = AdapterTasks(taskList)
        rvTodaysTasks.adapter = taskAdapter

        // Setup Collab Requests RecyclerView
        rvCollabRequests.layoutManager = LinearLayoutManager(requireContext())
        collabAdapter = AdapterCollabRequest(collabList) {
            // Callback when a request is accepted or declined
            lastCollabDoc = null
            loadCollabRequests()
        }
        rvCollabRequests.adapter = collabAdapter

        // Pagination Scroll Listener for Collab Requests
        rvCollabRequests.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isCollabLoading && hasMoreCollabs) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10
                    ) {
                        loadCollabRequests(isMore = true)
                    }
                }
            }
        })

        // Load cached profile data first
        loadCachedData()

        // Initial data load
        loadUserProfileData()
        loadCollabRequests()
        loadStatCounts()
        loadTodaysTasks()

        return view
    }

    private fun loadCachedData() {
        val name = preferenceManager.getUserName()
        tvGreeting.text = "Hello $name"
        tvActiveTasksCount.text = preferenceManager.getTasksCount().toString()
        tvActiveCollabsCount.text = preferenceManager.getCollabsCount().toString()

        val imageUrl = preferenceManager.getUserImage()
        if (!imageUrl.isNullOrEmpty()) {
            displayProfileImage(imageUrl)
        }

        // Use pre-fetched data from Splash
        if (DataRepository.cachedTasks.isNotEmpty()) {
            taskList.clear()
            taskList.addAll(DataRepository.cachedTasks)
            taskAdapter.notifyDataSetChanged()
        }

        if (DataRepository.cachedCollabRequests.isNotEmpty()) {
            collabList.clear()
            collabList.addAll(DataRepository.cachedCollabRequests)
            collabAdapter.notifyDataSetChanged()
            tvNewCollabs.text = "${collabList.size} new"
            llEmptyCollabs.visibility = View.GONE
            rvCollabRequests.visibility = View.VISIBLE
        }
    }

    private fun showLoadingDialog() {
        if (progressDialog == null) {
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
            builder.setView(dialogView)
            builder.setCancelable(false)
            progressDialog = builder.create()
            progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        progressDialog?.show()
    }

    private fun hideLoadingDialog() {
        progressDialog?.dismiss()
    }

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "Name"
                    val imageUrl = document.getString("profileImageUrl")
                    val bio = document.getString("bio") ?: ""
                    val followers = document.getLong("followers") ?: 0L

                    tvGreeting.text = "Hello ${name}"

                    // Update cache (name, image, bio, followers)
                    preferenceManager.saveUserProfile(name, bio, imageUrl, followers)

                    if (!imageUrl.isNullOrEmpty()) {
                        displayProfileImage(imageUrl)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    showToast("Failed to load profile: ${e.message}")
                }
            }
    }

    private fun displayProfileImage(imageUrl: String) {
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
                Log.e("HomeFragment", "Error decoding base64 image", e)
            }
        }
    }

    private fun loadCollabRequests(isMore: Boolean = false) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (isCollabLoading) return

        if (isMore) {
            showLoadingDialog()
        } else {
            pbHome.visibility = View.VISIBLE
            llEmptyCollabs.visibility = View.GONE
        }
        isCollabLoading = true

        var query = db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "Pending")
            .limit(10)

        if (isMore && lastCollabDoc != null) {
            query = query.startAfter(lastCollabDoc!!)
        }

        query.get()
            .addOnSuccessListener { documents ->
                isCollabLoading = false
                hideLoadingDialog()
                pbHome.visibility = View.GONE
                
                if (isAdded) {
                    if (!isMore) {
                        collabList.clear()
                        DataRepository.cachedCollabRequests.clear()
                    }

                    for (doc in documents) {
                        val request = ModelCollabRequest(
                            id = doc.id,
                            coProfileImg = doc.getString("senderImage") ?: "",
                            coName = doc.getString("senderName") ?: "Name",
                            coDescription = "Collab on: ${doc.getString("taskTitle") ?: "Project"}",
                            senderId = doc.getString("senderId") ?: "",
                            taskId = doc.getString("taskId") ?: "",
                            status = doc.getString("status") ?: "Pending"
                        )
                        collabList.add(request)
                        
                        // Keep cache updated with first 10
                        if (!isMore && collabList.size <= 10) {
                            DataRepository.cachedCollabRequests.add(request)
                        }
                    }

                    if (documents.size() > 0) {
                        lastCollabDoc = documents.documents[documents.size() - 1]
                        hasMoreCollabs = documents.size() == 10
                    } else {
                        hasMoreCollabs = false
                    }

                    collabAdapter.notifyDataSetChanged()
                    tvNewCollabs.text = "${collabList.size}${if (hasMoreCollabs) "+" else ""} new"
                    
                    // Toggle empty state visibility
                    if (collabList.isEmpty()) {
                        llEmptyCollabs.visibility = View.VISIBLE
                        rvCollabRequests.visibility = View.GONE
                    } else {
                        llEmptyCollabs.visibility = View.GONE
                        rvCollabRequests.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                isCollabLoading = false
                hideLoadingDialog()
                pbHome.visibility = View.GONE
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
                    val count = documents.size()
                    tvActiveTasksCount.text = count.toString()
                    preferenceManager.saveTasksCount(count)
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
                    preferenceManager.saveCollabsCount(count)
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

    private fun loadTodaysTasks() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // 1. Fetch user's own active tasks
        val ownTasksQuery = db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("status", "Active")
            .limit(10) // Limit for initial load
            .get()

        // 2. Fetch user's active collaborations
        val collabsQuery = db.collection(Constants.COLLECTION_COLLABS)
            .whereIn("status", listOf("Accepted", "Running"))
            .limit(10) // Limit for initial load
            .get()

        com.google.android.gms.tasks.Tasks.whenAllComplete(ownTasksQuery, collabsQuery)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                
                taskList.clear()
                DataRepository.cachedTasks.clear()
                
                // Add own tasks
                val ownDocs = ownTasksQuery.result?.documents ?: emptyList()
                for (doc in ownDocs) {
                    val task = ModelTasks(
                        id = doc.id,
                        taskImage = doc.getString("userProfileImage") ?: "",
                        taskTitle = doc.getString("projectTitle") ?: "Task",
                        taskDescription = doc.getString("description") ?: "",
                        taskStatus = "Active",
                        type = "task",
                        userId = currentUserId
                    )
                    taskList.add(task)
                    DataRepository.cachedTasks.add(task)
                }
                
                // Add collaborating tasks
                val collabDocs = collabsQuery.result?.documents ?: emptyList()
                for (doc in collabDocs) {
                    val sId = doc.getString("senderId") ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    
                    // Only add if I am a participant
                    if (sId == currentUserId || rId == currentUserId) {
                        val status = doc.getString("status") ?: "Running"
                        
                        // Show partner's image and link to their profile
                        val partnerId = if (sId == currentUserId) rId else sId
                        val partnerImage = if (sId == currentUserId) {
                            doc.getString("receiverImage") ?: ""
                        } else {
                            doc.getString("senderImage") ?: ""
                        }

                        val collabTask = ModelTasks(
                            id = doc.id,
                            taskImage = partnerImage,
                            taskTitle = doc.getString("taskTitle") ?: "Collab",
                            taskDescription = "Working with ${if (sId == currentUserId) doc.getString("receiverName") else doc.getString("senderName")}",
                            taskStatus = status,
                            type = "collab",
                            userId = partnerId
                        )
                        taskList.add(collabTask)
                        if (DataRepository.cachedTasks.size < 10) {
                            DataRepository.cachedTasks.add(collabTask)
                        }
                    }
                }
                
                taskAdapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
        lastCollabDoc = null
        loadCollabRequests()
        loadStatCounts()
        loadTodaysTasks()
    }
}