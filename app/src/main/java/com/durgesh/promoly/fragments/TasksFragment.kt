package com.durgesh.promoly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterTasksPriority
import com.durgesh.promoly.model.ModelTaskPriority
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.FcmNotificationSender
import com.durgesh.promoly.util.showToast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TasksFragment : Fragment() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: AdapterTasksPriority
    private val taskList = mutableListOf<ModelTaskPriority>()

    private lateinit var btnAllTasks: MaterialButton
    private lateinit var btnMyTasks: MaterialButton
    private lateinit var etSearchTasks: EditText
    private lateinit var btnSearchTasks: android.widget.ImageButton
    private var showingOnlyMyTasks = false
    private var taskListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        rvTasks = view.findViewById(R.id.rvTaskPriorities)
        btnAllTasks = view.findViewById(R.id.btnAllTasks)
        btnMyTasks = view.findViewById(R.id.btnMyTasks)
        etSearchTasks = view.findViewById(R.id.etSearchTasks)
        btnSearchTasks = view.findViewById(R.id.btnSearchTasks)

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdapterTasksPriority(taskList, FirebaseAuth.getInstance().currentUser?.uid ?: "") { task ->
            sendCollabRequest(task)
        }
        rvTasks.adapter = adapter

        btnAllTasks.setOnClickListener {
            if (showingOnlyMyTasks) {
                showingOnlyMyTasks = false
                updateFilterUI()
                loadTasksFromFirestore()
            }
        }

        btnMyTasks.setOnClickListener {
            if (!showingOnlyMyTasks) {
                showingOnlyMyTasks = true
                updateFilterUI()
                loadTasksFromFirestore()
            }
        }

        btnSearchTasks.setOnClickListener {
            val queryText = etSearchTasks.text.toString().trim()
            if (queryText.length >= 2) {
                searchTasks(queryText)
            } else {
                if (queryText.isEmpty()) {
                    loadTasksFromFirestore()
                } else {
                    showToast("Please enter at least 2 characters to search")
                }
            }
        }

        loadTasksFromFirestore()

        return view
    }

    private fun searchTasks(searchText: String) {
        val db = FirebaseFirestore.getInstance()
        
        // Search is always GLOBAL across all tasks as requested
        val query: Query = db.collection(Constants.COLLECTION_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Reset filter UI to "All Tasks" when searching globally
        showingOnlyMyTasks = false
        updateFilterUI()

        query.get().addOnSuccessListener { documents ->
            taskList.clear()
            val lowerSearch = searchText.lowercase()
            for (doc in documents) {
                val title = doc.getString("projectTitle") ?: ""
                val category = doc.getString("category") ?: ""
                val priority = doc.getString("priority") ?: "Medium Priority"

                // Local filtering for partial matches across multiple fields
                if (title.lowercase().contains(lowerSearch) || 
                    category.lowercase().contains(lowerSearch) || 
                    priority.lowercase().contains(lowerSearch)) {
                    
                    val task = mapDocumentToModel(doc)
                    taskList.add(task)
                }
            }
            adapter.notifyDataSetChanged()
            if (taskList.isEmpty() && isAdded) {
                showToast("No tasks found matching '$searchText'")
            }
        }
    }

    private fun mapDocumentToModel(doc: com.google.firebase.firestore.DocumentSnapshot): ModelTaskPriority {
        return ModelTaskPriority(
            id = doc.getString("id") ?: doc.id,
            userId = doc.getString("userId") ?: "",
            userName = doc.getString("userName") ?: "User",
            userProfileImage = doc.getString("userProfileImage") ?: "",
            priority = doc.getString("priority") ?: "Medium Priority",
            category = doc.getString("category") ?: "",
            title = doc.getString("projectTitle") ?: "",
            description = doc.getString("description") ?: "",
            timeline = doc.getString("timeline") ?: "",
            budget = doc.getString("budget") ?: "",
            progress = doc.getLong("progress")?.toInt() ?: 0,
            status = doc.getString("status") ?: "Active",
            skills = doc.get("skills") as? List<String> ?: emptyList()
        )
    }

    private fun updateFilterUI() {
        val activeColor = resources.getColor(R.color.lightgreen, null)
        val inactiveColor = android.graphics.Color.parseColor("#F2F4F7")
        val activeTextColor = resources.getColor(R.color.green, null)
        val inactiveTextColor = resources.getColor(R.color.grey, null)

        if (showingOnlyMyTasks) {
            btnMyTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
            btnMyTasks.setTextColor(activeTextColor)
            btnMyTasks.elevation = resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
            
            btnAllTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnAllTasks.setTextColor(inactiveTextColor)
            btnAllTasks.elevation = 0f
        } else {
            btnAllTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
            btnAllTasks.setTextColor(activeTextColor)
            btnAllTasks.elevation = resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
            
            btnMyTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnMyTasks.setTextColor(inactiveTextColor)
            btnMyTasks.elevation = 0f
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasksFromFirestore()
    }

    private fun sendCollabRequest(task: ModelTaskPriority) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. Check if a request already exists to avoid spamming
        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("taskId", task.id)
            .whereEqualTo("senderId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    showToast("Request already sent for this task!")
                    return@addOnSuccessListener
                }

                // 2. Fetch sender's info (current user) to denormalize into the request
                db.collection(Constants.COLLECTION_USERS).document(currentUserId).get()
                    .addOnSuccessListener { senderDoc ->
                        val senderName = senderDoc.getString("name") ?: "User"
                        val senderImage = senderDoc.getString("profileImageUrl") ?: ""

                        val requestMap = hashMapOf(
                            "taskId" to task.id,
                            "taskTitle" to task.title,
                            "senderId" to currentUserId,
                            "senderName" to senderName,
                            "senderImage" to senderImage,
                            "receiverId" to task.userId, // The one who posted the task
                            "status" to "Pending",
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        db.collection(Constants.COLLECTION_COLLABS)
                            .add(requestMap)
                            .addOnSuccessListener {
                                showToast("Collaboration request sent!")
                                // Trigger notification to task owner
                                FcmNotificationSender.sendNotification(
                                    receiverId = task.userId,
                                    title = "New Collaboration Request",
                                    message = "$senderName wants to collaborate on \"${task.title}\""
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e("TasksFragment", "Error sending request", e)
                                showToast("Failed to send request")
                            }
                    }
            }
    }

    private fun loadTasksFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Remove old listener if it exists
        taskListener?.remove()

        // Use addSnapshotListener for "live" updates
        taskListener = db.collection(Constants.COLLECTION_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("TasksFragment", "Listen failed.", e)
                    if (isAdded) showToast("Failed to load live updates")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    taskList.clear()
                    for (doc in snapshots) {
                        try {
                            val taskUserId = doc.getString("userId") ?: ""
                            
                            // Apply filter in memory if "My Tasks" is selected
                            if (showingOnlyMyTasks && taskUserId != currentUserId) {
                                continue
                            }

                            val task = mapDocumentToModel(doc)
                            taskList.add(task)
                        } catch (e: Exception) {
                            Log.e("TasksFragment", "Error parsing task: ${e.message}")
                        }
                    }
                    adapter.notifyDataSetChanged()
                    
                    if (taskList.isEmpty() && isAdded) {
                        Log.d("TasksFragment", if (showingOnlyMyTasks) "No personal tasks found" else "No global tasks found")
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        taskListener?.remove()
    }
}
