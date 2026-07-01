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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

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

        ivProfileImage.setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav?.selectedItemId = R.id.navigation_profile
        }

        view.findViewById<View>(R.id.cvSearchHome).setOnClickListener {
            startActivity(Intent(requireContext(), SearchUserActivity::class.java))
        }

        rvTodaysTasks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        taskAdapter = AdapterTasks(taskList)
        rvTodaysTasks.adapter = taskAdapter

        rvCollabRequests.layoutManager = LinearLayoutManager(requireContext())
        collabAdapter = AdapterCollabRequest(collabList) {
            lastCollabDoc = null
            loadCollabRequests()
        }
        rvCollabRequests.adapter = collabAdapter

        rvCollabRequests.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (!isCollabLoading && hasMoreCollabs && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && totalItemCount >= 10) {
                    loadCollabRequests(isMore = true)
                }
            }
        })

        loadCachedData()
        loadUserProfileData()
        loadCollabRequests()
        loadStatCounts()
        loadTodaysTasks()

        return view
    }

    private fun loadCachedData() {
        tvGreeting.text = "Hello ${preferenceManager.getUserName()}"
        tvActiveTasksCount.text = preferenceManager.getTasksCount().toString()
        tvActiveCollabsCount.text = preferenceManager.getCollabsCount().toString()
        preferenceManager.getUserImage()?.let { displayProfileImage(it) }

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

    private fun loadUserProfileData() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection(Constants.COLLECTION_USERS).document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name") ?: "Name"
                    val imageUrl = document.getString("profileImageUrl")
                    val bio = document.getString("bio") ?: ""
                    val followers = document.getLong("followers") ?: 0L
                    tvGreeting.text = "Hello $name"
                    preferenceManager.saveUserProfile(name, bio, imageUrl, followers)
                    imageUrl?.let { displayProfileImage(it) }
                }
            }
    }

    private fun displayProfileImage(imageUrl: String) {
        if (imageUrl.startsWith("http")) {
            Glide.with(this).load(imageUrl).into(ivProfileImage)
        } else {
            try {
                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfileImage.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                ivProfileImage.setImageResource(R.drawable.user)
            }
        }
    }

    private fun loadCollabRequests(isMore: Boolean = false) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (isCollabLoading) return
        if (isMore) showLoadingDialog() else {
            pbHome.visibility = View.VISIBLE
            llEmptyCollabs.visibility = View.GONE
        }
        isCollabLoading = true
        var query = db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "Pending")
            .limit(10)
        if (isMore && lastCollabDoc != null) query = query.startAfter(lastCollabDoc!!)

        query.get().addOnSuccessListener { documents ->
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
                    if (!isMore && collabList.size <= 10) DataRepository.cachedCollabRequests.add(request)
                }
                if (documents.size() > 0) {
                    lastCollabDoc = documents.documents[documents.size() - 1]
                    hasMoreCollabs = documents.size() == 10
                } else hasMoreCollabs = false
                collabAdapter.notifyDataSetChanged()
                tvNewCollabs.text = "${collabList.size}${if (hasMoreCollabs) "+" else ""} new"
                if (collabList.isEmpty()) {
                    llEmptyCollabs.visibility = View.VISIBLE
                    rvCollabRequests.visibility = View.GONE
                } else {
                    llEmptyCollabs.visibility = View.GONE
                    rvCollabRequests.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadStatCounts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection(Constants.COLLECTION_TASKS).whereEqualTo("userId", userId).get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    tvActiveTasksCount.text = documents.size().toString()
                    preferenceManager.saveTasksCount(documents.size())
                }
            }
        db.collection(Constants.COLLECTION_COLLABS).whereEqualTo("status", "Accepted").get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    val count = documents.count { it.getString("senderId") == userId || it.getString("receiverId") == userId }
                    tvActiveCollabsCount.text = count.toString()
                    preferenceManager.saveCollabsCount(count)
                }
            }
        db.collection(Constants.COLLECTION_COLLABS).whereEqualTo("status", "Completed").get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    tvCompletedCollabsCount.text = documents.count { it.getString("senderId") == userId || it.getString("receiverId") == userId }.toString()
                }
            }
    }

    private fun loadTodaysTasks() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Fetch user's own tasks without "Active" filter in query to avoid index requirement
        val ownTasksQuery = db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", currentUserId)
            .limit(10)
            .get()

        val collabsQuery = db.collection(Constants.COLLECTION_COLLABS)
            .whereIn("status", listOf("Accepted", "Running"))
            .limit(10)
            .get()

        com.google.android.gms.tasks.Tasks.whenAllComplete(ownTasksQuery, collabsQuery)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                
                taskList.clear()
                DataRepository.cachedTasks.clear()
                
                ownTasksQuery.result?.documents?.forEach { doc ->
                    val status = doc.getString("status") ?: "Active"
                    if (status == "Active") {
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
                }
                
                collabsQuery.result?.documents?.forEach { doc ->
                    val sId = doc.getString("senderId") ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    if (sId == currentUserId || rId == currentUserId) {
                        val partnerId = if (sId == currentUserId) rId else sId
                        val partnerImage = if (sId == currentUserId) doc.getString("receiverImage") else doc.getString("senderImage")
                        val collabTask = ModelTasks(
                            id = doc.id,
                            taskImage = partnerImage ?: "",
                            taskTitle = doc.getString("taskTitle") ?: "Collab",
                            taskDescription = "Working with ${if (sId == currentUserId) doc.getString("receiverName") else doc.getString("senderName")}",
                            taskStatus = doc.getString("status") ?: "Running",
                            type = "collab",
                            userId = partnerId
                        )
                        taskList.add(collabTask)
                        if (DataRepository.cachedTasks.size < 10) DataRepository.cachedTasks.add(collabTask)
                    }
                }
                taskAdapter.notifyDataSetChanged()
            }
    }

    private fun showLoadingDialog() {
        if (progressDialog == null) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            builder.setCancelable(false)
            progressDialog = builder.create()
            progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        progressDialog?.show()
    }

    private fun hideLoadingDialog() = progressDialog?.dismiss()

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
        lastCollabDoc = null
        loadCollabRequests()
        loadStatCounts()
        loadTodaysTasks()
    }
}
