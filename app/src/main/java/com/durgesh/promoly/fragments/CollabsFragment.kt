package com.durgesh.promoly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterCollabProgress
import com.durgesh.promoly.model.ModelCollabProgress

import android.util.Log
import com.durgesh.promoly.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CollabsFragment : Fragment() {

    private lateinit var rvCollabProgress: RecyclerView
    private lateinit var adapter: AdapterCollabProgress
    private val progressList = mutableListOf<ModelCollabProgress>()
    private lateinit var db: FirebaseFirestore

    private lateinit var btnAll: com.google.android.material.button.MaterialButton
    private lateinit var btnMyCollab: com.google.android.material.button.MaterialButton
    private lateinit var btnActive: com.google.android.material.button.MaterialButton
    private lateinit var btnRunning: com.google.android.material.button.MaterialButton
    private lateinit var btnCompleted: com.google.android.material.button.MaterialButton
    
    private var currentFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_collabs, container, false)

        db = FirebaseFirestore.getInstance()
        rvCollabProgress = view.findViewById(R.id.rvCollabRequests)
        
        btnAll = view.findViewById(R.id.btnAllCollab)
        btnMyCollab = view.findViewById(R.id.btnMyCollab)
        btnActive = view.findViewById(R.id.btnActiveCollab)
        btnRunning = view.findViewById(R.id.btnPendingCollab)
        btnCompleted = view.findViewById(R.id.btnCompletedCollab)

        rvCollabProgress.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdapterCollabProgress(progressList)
        rvCollabProgress.adapter = adapter

        setupFilterButtons()
        loadCollaborations()

        return view
    }

    private fun setupFilterButtons() {
        btnAll.setOnClickListener { updateFilter("All") }
        btnMyCollab.setOnClickListener { updateFilter("My Collabs") }
        btnActive.setOnClickListener { updateFilter("Accepted") }
        btnRunning.setOnClickListener { updateFilter("Running") }
        btnCompleted.setOnClickListener { updateFilter("Completed") }
    }

    private fun updateFilter(filter: String) {
        currentFilter = filter
        updateFilterUI()
        loadCollaborations()
    }

    private fun updateFilterUI() {
        val activeColor = resources.getColor(R.color.green, null)
        val inactiveColor = android.graphics.Color.parseColor("#F2F4F7")
        val activeTextColor = resources.getColor(R.color.white, null)
        val inactiveTextColor = resources.getColor(R.color.assetgrey, null)

        val buttons = listOf(btnAll, btnMyCollab, btnActive, btnRunning, btnCompleted)
        val filterNames = listOf("All", "My Collabs", "Accepted", "Running", "Completed")

        buttons.forEachIndexed { index, button ->
            if (filterNames[index] == currentFilter) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                button.setTextColor(activeTextColor)
                button.elevation = resources.getDimension(com.intuit.sdp.R.dimen._3sdp)
            } else {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
                button.setTextColor(inactiveTextColor)
                button.elevation = resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
            }
        }
    }

    private fun loadCollaborations() {
        var query: com.google.firebase.firestore.Query = db.collection(Constants.COLLECTION_COLLABS)
        
        // If we want a specific status, filter by it in Firestore first
        if (currentFilter != "All" && currentFilter != "My Collabs") {
            query = query.whereEqualTo("status", currentFilter)
        }

        query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e("CollabsFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                progressList.clear()
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                // Convert to list and sort in memory to avoid index requirements
                val sortedDocs = snapshots.documents.sortedByDescending { 
                    it.getTimestamp("acceptedAt")?.toDate()?.time ?: it.getTimestamp("timestamp")?.toDate()?.time ?: 0L 
                }

                for (doc in sortedDocs) {
                    try {
                        val senderId = doc.getString("senderId") ?: ""
                        val receiverId = doc.getString("receiverId") ?: ""
                        val status = doc.getString("status") ?: ""
                        
                        // Filtering logic
                        if (currentFilter == "My Collabs") {
                            if (senderId != currentUserId && receiverId != currentUserId) continue
                        }

                        val senderName = doc.getString("senderName") ?: "User"
                        val receiverName = doc.getString("receiverName") ?: "User"
                        
                        // Create a display name like "Sarah & You" or "Sarah & Alex"
                        val displayName = if (senderId == currentUserId) {
                            "You & $receiverName"
                        } else if (receiverId == currentUserId) {
                            "$senderName & You"
                        } else {
                            "$senderName & $receiverName"
                        }

                        val progress = ModelCollabProgress(
                            id = doc.id,
                            copProfileImg1 = doc.getString("senderImage") ?: "",
                            copProfileImg2 = doc.getString("receiverImage") ?: "",
                            copName = displayName,
                            copDescription = "Status: $status",
                            copProjectTitle = doc.getString("taskTitle") ?: "Project",
                            copCategory = status,
                            copProgress = doc.getLong("progress")?.toInt() ?: 0
                        )
                        progressList.add(progress)
                    } catch (ex: Exception) {
                        Log.e("CollabsFragment", "Error parsing collab: ${ex.message}")
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }
    }
}