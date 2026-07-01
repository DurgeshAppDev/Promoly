package com.durgesh.promoly.fragments

import android.os.Bundle
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
    private lateinit var pbTasks: android.widget.ProgressBar
    private lateinit var llEmptyTasks: View
    private var showingOnlyMyTasks = false
    private var taskListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        rvTasks = view.findViewById(R.id.rvTaskPriorities)
        btnAllTasks = view.findViewById(R.id.btnAllTasks)
        btnMyTasks = view.findViewById(R.id.btnMyTasks)
        etSearchTasks = view.findViewById(R.id.etSearchTasks)
        btnSearchTasks = view.findViewById(R.id.btnSearchTasks)
        pbTasks = view.findViewById(R.id.pbTasks)
        llEmptyTasks = view.findViewById(R.id.llEmptyTasks)

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdapterTasksPriority(taskList, FirebaseAuth.getInstance().currentUser?.uid ?: "") { sendCollabRequest(it) }
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
            if (queryText.length >= 2) searchTasks(queryText) else if (queryText.isEmpty()) loadTasksFromFirestore() else showToast("Min 2 chars")
        }
        return view
    }

    private fun searchTasks(searchText: String) {
        val db = FirebaseFirestore.getInstance()
        pbTasks.visibility = View.VISIBLE
        llEmptyTasks.visibility = View.GONE
        rvTasks.visibility = View.GONE
        showingOnlyMyTasks = false
        updateFilterUI()

        // Simplified query to avoid index requirements
        db.collection(Constants.COLLECTION_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
            pbTasks.visibility = View.GONE
            taskList.clear()
            val lowerSearch = searchText.lowercase()
            for (doc in documents) {
                val status = doc.getString("status") ?: "Active"
                if (status != "Active") continue

                val title = doc.getString("projectTitle") ?: ""
                val category = doc.getString("category") ?: ""
                val priority = doc.getString("priority") ?: "Medium Priority"
                if (title.lowercase().contains(lowerSearch) || category.lowercase().contains(lowerSearch) || priority.lowercase().contains(lowerSearch)) {
                    taskList.add(mapDocumentToModel(doc))
                }
            }
            adapter.notifyDataSetChanged()
            if (taskList.isEmpty()) {
                llEmptyTasks.visibility = View.VISIBLE
                rvTasks.visibility = View.GONE
            } else {
                llEmptyTasks.visibility = View.GONE
                rvTasks.visibility = View.VISIBLE
            }
        }
    }

    private fun mapDocumentToModel(doc: com.google.firebase.firestore.DocumentSnapshot): ModelTaskPriority {
        return ModelTaskPriority(id = doc.getString("id") ?: doc.id, userId = doc.getString("userId") ?: "", userName = doc.getString("userName") ?: "Name", userProfileImage = doc.getString("userProfileImage") ?: "", priority = doc.getString("priority") ?: "Medium Priority", category = doc.getString("category") ?: "", title = doc.getString("projectTitle") ?: "", description = doc.getString("description") ?: "", timeline = doc.getString("timeline") ?: "", budget = doc.getString("budget") ?: "", progress = doc.getLong("progress")?.toInt() ?: 0, status = doc.getString("status") ?: "Active", skills = doc.get("skills") as? List<String> ?: emptyList())
    }

    private fun updateFilterUI() {
        val activeColor = resources.getColor(R.color.lightgreen, null)
        val inactiveColor = android.graphics.Color.parseColor("#F2F4F7")
        val activeTextColor = resources.getColor(R.color.green, null)
        val inactiveTextColor = resources.getColor(R.color.grey, null)

        if (showingOnlyMyTasks) {
            btnMyTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
            btnMyTasks.setTextColor(activeTextColor)
            btnAllTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnAllTasks.setTextColor(inactiveTextColor)
        } else {
            btnAllTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
            btnAllTasks.setTextColor(activeTextColor)
            btnMyTasks.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnMyTasks.setTextColor(inactiveTextColor)
        }
    }

    private fun sendCollabRequest(task: ModelTaskPriority) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLLECTION_COLLABS).whereEqualTo("taskId", task.id).whereEqualTo("senderId", currentUserId).get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                showToast("Already sent")
                return@addOnSuccessListener
            }
            db.collection(Constants.COLLECTION_USERS).document(currentUserId).get().addOnSuccessListener { senderDoc ->
                val senderName = senderDoc.getString("name") ?: "Name"
                val requestMap = hashMapOf("taskId" to task.id, "taskTitle" to task.title, "senderId" to currentUserId, "senderName" to senderName, "senderImage" to (senderDoc.getString("profileImageUrl") ?: ""), "receiverId" to task.userId, "status" to "Pending", "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                db.collection(Constants.COLLECTION_COLLABS).add(requestMap).addOnSuccessListener {
                    if (isAdded) {
                        showToast("Request sent!")
                        context?.let { ctx -> FcmNotificationSender.sendNotification(ctx, task.userId, "New Request", "$senderName wants to collaborate", "collab_request") }
                    }
                }
            }
        }
    }

    private fun loadTasksFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        pbTasks.visibility = View.VISIBLE
        llEmptyTasks.visibility = View.GONE
        rvTasks.visibility = View.GONE
        taskListener?.remove()
        
        // Simplified query to avoid blank screens/hangs from missing indexes
        taskListener = db.collection(Constants.COLLECTION_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
            pbTasks.visibility = View.GONE
            if (e != null || snapshots == null) {
                llEmptyTasks.visibility = View.VISIBLE
                return@addSnapshotListener
            }
            taskList.clear()
            for (doc in snapshots) {
                val status = doc.getString("status") ?: "Active"
                if (status == "Active") {
                    if (showingOnlyMyTasks && doc.getString("userId") != currentUserId) continue
                    taskList.add(mapDocumentToModel(doc))
                }
            }
            adapter.notifyDataSetChanged()
            if (taskList.isEmpty()) {
                llEmptyTasks.visibility = View.VISIBLE
                rvTasks.visibility = View.GONE
            } else {
                llEmptyTasks.visibility = View.GONE
                rvTasks.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasksFromFirestore()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        taskListener?.remove()
    }
}
