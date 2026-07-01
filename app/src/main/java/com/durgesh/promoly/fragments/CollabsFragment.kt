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
import com.durgesh.promoly.util.showToast
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
    private lateinit var pbCollabs: android.widget.ProgressBar
    private lateinit var llEmptyCollabs: View
    private var currentFilter = "All"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_collabs, container, false)
        db = FirebaseFirestore.getInstance()
        rvCollabProgress = view.findViewById(R.id.rvCollabRequests)
        btnAll = view.findViewById(R.id.btnAllCollab)
        btnMyCollab = view.findViewById(R.id.btnMyCollab)
        btnActive = view.findViewById(R.id.btnActiveCollab)
        btnRunning = view.findViewById(R.id.btnPendingCollab)
        btnCompleted = view.findViewById(R.id.btnCompletedCollab)
        pbCollabs = view.findViewById(R.id.pbCollabs)
        llEmptyCollabs = view.findViewById(R.id.llEmptyCollabs)

        rvCollabProgress.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdapterCollabProgress(progressList) { showUpdateProgressDialog(it) }
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
        val buttons = listOf(btnAll, btnMyCollab, btnActive, btnRunning, btnCompleted)
        val filterNames = listOf("All", "My Collabs", "Accepted", "Running", "Completed")

        buttons.forEachIndexed { index, button ->
            val isActive = filterNames[index] == currentFilter
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isActive) activeColor else inactiveColor)
            button.setTextColor(if (isActive) resources.getColor(R.color.white, null) else resources.getColor(R.color.assetgrey, null))
        }
    }

    private fun showUpdateProgressDialog(collab: ModelCollabProgress) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (collab.senderId != currentUserId && collab.receiverId != currentUserId) {
            showToast("No permission")
            return
        }
        val options = arrayOf("Starting (25%)", "Mid (50%)", "Just Ending (75%)", "Finished (100%)")
        val progressValues = intArrayOf(25, 50, 75, 100)
        android.app.AlertDialog.Builder(requireContext()).setTitle("Update Progress").setItems(options) { _, which ->
            updateCollabProgress(collab.id, progressValues[which])
        }.setNegativeButton("Cancel", null).show()
    }

    private fun updateCollabProgress(collabId: String, newProgress: Int) {
        val updates = hashMapOf<String, Any>("progress" to newProgress)
        updates["status"] = if (newProgress == 100) "Completed" else "Running"
        db.collection(Constants.COLLECTION_COLLABS).document(collabId).update(updates).addOnSuccessListener {
            showToast("Updated!")
        }
    }

    private fun loadCollaborations() {
        pbCollabs.visibility = View.VISIBLE
        llEmptyCollabs.visibility = View.GONE
        rvCollabProgress.visibility = View.GONE

        var query: com.google.firebase.firestore.Query = db.collection(Constants.COLLECTION_COLLABS)
        if (currentFilter != "All" && currentFilter != "My Collabs") query = query.whereEqualTo("status", currentFilter)

        query.addSnapshotListener { snapshots, e ->
            pbCollabs.visibility = View.GONE
            if (e != null || snapshots == null) {
                llEmptyCollabs.visibility = View.VISIBLE
                return@addSnapshotListener
            }
            progressList.clear()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val sortedDocs = snapshots.documents.sortedByDescending { it.getTimestamp("acceptedAt")?.toDate()?.time ?: it.getTimestamp("timestamp")?.toDate()?.time ?: 0L }

            for (doc in sortedDocs) {
                val sId = doc.getString("senderId") ?: ""
                val rId = doc.getString("receiverId") ?: ""
                val status = doc.getString("status") ?: ""
                if (currentFilter == "All" && (status == "Completed" || status == "Declined" || status == "Pending")) continue
                if (currentFilter == "My Collabs" && sId != currentUserId && rId != currentUserId) continue

                val sName = doc.getString("senderName") ?: "Name"
                val rName = doc.getString("receiverName") ?: "Name"
                val displayName = if (sId == currentUserId) "You & $rName" else if (rId == currentUserId) "$sName & You" else "$sName & $rName"

                progressList.add(ModelCollabProgress(id = doc.id, copProfileImg1 = doc.getString("senderImage") ?: "", copProfileImg2 = doc.getString("receiverImage") ?: "", copName = displayName, copDescription = "Status: $status", copProjectTitle = doc.getString("taskTitle") ?: "Project", copCategory = status, copProgress = doc.getLong("progress")?.toInt() ?: 0, senderId = sId, receiverId = rId))
            }
            adapter.notifyDataSetChanged()
            if (progressList.isEmpty()) {
                llEmptyCollabs.visibility = View.VISIBLE
                rvCollabProgress.visibility = View.GONE
            } else {
                llEmptyCollabs.visibility = View.GONE
                rvCollabProgress.visibility = View.VISIBLE
            }
        }
    }
}
