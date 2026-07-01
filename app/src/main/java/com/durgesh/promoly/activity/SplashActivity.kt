package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.model.ModelTasks
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.DataRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            // Silently handle
        }

        setContentView(R.layout.activity_splash)
        val logo = findViewById<ImageView>(R.id.logo)
        val appName = findViewById<TextView>(R.id.tvAppName)
        val tagline = findViewById<TextView>(R.id.tvTagline)

        logo.alpha = 0f
        appName.alpha = 0f
        tagline.alpha = 0f

        logo.animate().alpha(1f).setDuration(800).start()
        appName.animate().alpha(1f).setDuration(1500).start()
        tagline.animate().alpha(1f).setDuration(1500).start()

        prefetchData()

        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }

    private fun prefetchData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val ownTasksTask = db.collection(Constants.COLLECTION_TASKS).whereEqualTo("userId", currentUserId).limit(10).get()
        val activeCollabsTask = db.collection(Constants.COLLECTION_COLLABS).whereIn("status", listOf("Accepted", "Running")).limit(10).get()
        val pendingRequestsTask = db.collection(Constants.COLLECTION_COLLABS).whereEqualTo("receiverId", currentUserId).whereEqualTo("status", "Pending").limit(10).get()

        com.google.android.gms.tasks.Tasks.whenAllComplete(ownTasksTask, activeCollabsTask, pendingRequestsTask).addOnSuccessListener {
            DataRepository.cachedTasks.clear()
            DataRepository.cachedCollabRequests.clear()

            ownTasksTask.result?.documents?.forEach { doc ->
                val status = doc.getString("status") ?: "Active"
                if (status == "Active") {
                    DataRepository.cachedTasks.add(ModelTasks(id = doc.id, taskImage = doc.getString("userProfileImage") ?: "", taskTitle = doc.getString("projectTitle") ?: "Task", taskDescription = doc.getString("description") ?: "", taskStatus = "Active", type = "task", userId = currentUserId))
                }
            }

            activeCollabsTask.result?.documents?.forEach { doc ->
                val sId = doc.getString("senderId") ?: ""
                val rId = doc.getString("receiverId") ?: ""
                if (sId == currentUserId || rId == currentUserId) {
                    val partnerId = if (sId == currentUserId) rId else sId
                    val partnerImage = if (sId == currentUserId) doc.getString("receiverImage") else doc.getString("senderImage")
                    DataRepository.cachedTasks.add(ModelTasks(id = doc.id, taskImage = partnerImage ?: "", taskTitle = doc.getString("taskTitle") ?: "Collab", taskDescription = "Working with ${if (sId == currentUserId) doc.getString("receiverName") else doc.getString("senderName")}", taskStatus = doc.getString("status") ?: "Running", type = "collab", userId = partnerId))
                }
            }

            pendingRequestsTask.result?.documents?.forEach { doc ->
                DataRepository.cachedCollabRequests.add(ModelCollabRequest(id = doc.id, coProfileImg = doc.getString("senderImage") ?: "", coName = doc.getString("senderName") ?: "Name", coDescription = "Collab on: ${doc.getString("taskTitle") ?: "Project"}", senderId = doc.getString("senderId") ?: "", taskId = doc.getString("taskId") ?: "", status = "Pending"))
            }

            DataRepository.isDataLoaded = true
        }
    }
}
