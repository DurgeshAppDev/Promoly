package com.durgesh.promoly.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterCollabProgress
import com.durgesh.promoly.adapter.AdapterTasksPriority
import com.durgesh.promoly.model.ModelCollabProgress
import com.durgesh.promoly.model.ModelTaskPriority
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ViewProfile : AppCompatActivity() {
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvProfileCollabsCount: TextView
    private lateinit var tvProfileFollowersCount: TextView
    private lateinit var tvProfileTasksCount: TextView
    private lateinit var rvCollabs: RecyclerView
    private lateinit var rvTasks: RecyclerView
    private lateinit var collabAdapter: AdapterCollabProgress
    private lateinit var taskAdapter: AdapterTasksPriority
    private val collabList = mutableListOf<ModelCollabProgress>()
    private val taskList = mutableListOf<ModelTaskPriority>()
    private lateinit var db: FirebaseFirestore
    private var profileUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_profile)

        db = FirebaseFirestore.getInstance()
        profileUserId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid

        if (profileUserId == null) {
            showToast("User not found")
            finish()
            return
        }

        initViews()
        setupRecyclerViews()
        loadProfileData()
        loadCollabs()
        loadTasks()
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileBio = findViewById(R.id.tvProfileBio)
        tvProfileCollabsCount = findViewById(R.id.tvProfileCollabsCount)
        tvProfileFollowersCount = findViewById(R.id.tvProfileFollowersCount)
        tvProfileTasksCount = findViewById(R.id.tvProfileTasksCount)
        rvCollabs = findViewById(R.id.rvCollabRequests)
        rvTasks = findViewById(R.id.rvTaskPrioritiesProfile)
        
        val btnFollow = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFollow)
        val btnMessage = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMessage)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressed() }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (profileUserId == currentUserId) {
            btnFollow.text = "Edit Profile"
            btnMessage.visibility = View.GONE
            btnFollow.setOnClickListener {
                startActivity(Intent(this, ProfileInformation::class.java))
            }
        } else {
            checkFollowStatus(btnFollow)
            btnFollow.setOnClickListener { toggleFollow(btnFollow) }
            btnMessage.setOnClickListener {
                val intent = Intent(this, ChatRoomActivity::class.java)
                intent.putExtra("receiverId", profileUserId)
                startActivity(intent)
            }
        }
    }

    private fun checkFollowStatus(button: com.google.android.material.button.MaterialButton) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection(Constants.COLLECTION_FOLLOWERS).document(profileUserId!!).collection("UserFollowers").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) setFollowingUI(button) else setFollowUI(button)
            }
    }

    private fun setFollowingUI(button: com.google.android.material.button.MaterialButton) {
        button.text = "Following"
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F2F4F7"))
        button.setTextColor(resources.getColor(R.color.assetgrey, null))
    }

    private fun setFollowUI(button: com.google.android.material.button.MaterialButton) {
        button.text = "Follow"
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.green, null))
        button.setTextColor(android.graphics.Color.WHITE)
    }

    private fun toggleFollow(button: com.google.android.material.button.MaterialButton) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileRef = db.collection(Constants.COLLECTION_FOLLOWERS).document(profileUserId!!)
        val followerRef = profileRef.collection("UserFollowers").document(currentUserId)

        if (button.text == "Follow") {
            followerRef.set(hashMapOf("timestamp" to FieldValue.serverTimestamp())).addOnSuccessListener {
                db.collection(Constants.COLLECTION_USERS).document(profileUserId!!).update("followers", FieldValue.increment(1))
            }
        } else {
            followerRef.delete().addOnSuccessListener {
                db.collection(Constants.COLLECTION_USERS).document(profileUserId!!).update("followers", FieldValue.increment(-1))
            }
        }
    }

    private fun setupRecyclerViews() {
        rvCollabs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        collabAdapter = AdapterCollabProgress(collabList) {}
        rvCollabs.adapter = collabAdapter

        rvTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = AdapterTasksPriority(taskList, FirebaseAuth.getInstance().currentUser?.uid ?: "") {}
        rvTasks.adapter = taskAdapter
    }

    private fun loadProfileData() {
        db.collection(Constants.COLLECTION_USERS).document(profileUserId!!)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: "Name"
                    val bio = doc.getString("bio") ?: ""
                    val imageUrl = doc.getString("profileImageUrl")
                    val followers = doc.getLong("followers") ?: 0L
                    tvProfileName.text = name
                    tvProfileBio.text = bio.ifEmpty { "Digital Creator & Brand Strategist." }
                    tvProfileFollowersCount.text = if (followers >= 1000) "${followers/1000}k" else followers.toString()
                    imageUrl?.let { displayImage(it) }
                }
            }
    }

    private fun displayImage(imageUrl: String) {
        if (imageUrl.startsWith("http")) Glide.with(this).load(imageUrl).placeholder(R.drawable.user).into(ivProfileImage)
        else {
            try {
                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfileImage.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                ivProfileImage.setImageResource(R.drawable.user)
            }
        }
    }

    private fun loadCollabs() {
        db.collection(Constants.COLLECTION_COLLABS).addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            collabList.clear()
            var completedCount = 0
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            for (doc in snapshots) {
                val sId = doc.getString("senderId") ?: ""
                val rId = doc.getString("receiverId") ?: ""
                val status = doc.getString("status") ?: ""
                if (sId == profileUserId || rId == profileUserId) {
                    if (status == "Accepted" || status == "Running" || status == "Completed") {
                        val sName = doc.getString("senderName") ?: "Name"
                        val rName = doc.getString("receiverName") ?: "Name"
                        val displayName = if (sId == currentUserId) "You & $rName" else if (rId == currentUserId) "$sName & You" else "$sName & $rName"
                        collabList.add(ModelCollabProgress(id = doc.id, copProfileImg1 = doc.getString("senderImage") ?: "", copProfileImg2 = doc.getString("receiverImage") ?: "", copName = displayName, copDescription = "Status: $status", copProjectTitle = doc.getString("taskTitle") ?: "Project", copCategory = status, copProgress = doc.getLong("progress")?.toInt() ?: 0, senderId = sId, receiverId = rId))
                        if (status == "Completed") completedCount++
                    }
                }
            }
            collabAdapter.notifyDataSetChanged()
            tvProfileCollabsCount.text = completedCount.toString()
        }
    }

    private fun loadTasks() {
        db.collection(Constants.COLLECTION_TASKS).whereEqualTo("userId", profileUserId).orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                taskList.clear()
                for (doc in snapshots) {
                    taskList.add(ModelTaskPriority(id = doc.id, userId = doc.getString("userId") ?: "", userName = doc.getString("userName") ?: "Name", userProfileImage = doc.getString("userProfileImage") ?: "", priority = doc.getString("priority") ?: "Medium Priority", category = doc.getString("category") ?: "", title = doc.getString("projectTitle") ?: "", description = doc.getString("description") ?: "", timeline = doc.getString("timeline") ?: "", budget = doc.getString("budget") ?: "", progress = doc.getLong("progress")?.toInt() ?: 0, status = doc.getString("status") ?: "Active", skills = doc.get("skills") as? List<String> ?: emptyList()))
                }
                taskAdapter.notifyDataSetChanged()
                tvProfileTasksCount.text = taskList.size.toString()
            }
    }
}
