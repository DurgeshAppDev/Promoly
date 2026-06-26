package com.durgesh.promoly.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
    private lateinit var ivBack: ImageButton
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
        ivBack = findViewById(R.id.btnBack)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileBio = findViewById(R.id.tvProfileBio)
        tvProfileCollabsCount = findViewById(R.id.tvProfileCollabsCount)
        tvProfileFollowersCount = findViewById(R.id.tvProfileFollowersCount)
        tvProfileTasksCount = findViewById(R.id.tvProfileTasksCount)
        
        val btnFollow = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFollow)
        val btnMessage = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMessage)

        rvCollabs = findViewById(R.id.rvCollabRequests)
        rvTasks = findViewById(R.id.rvTaskPrioritiesProfile)

        ivBack.setOnClickListener {
            onBackPressed()
        }

        // Hide follow/message if viewing own profile
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (profileUserId == currentUserId) {
            btnFollow.text = "Edit Profile"
            btnMessage.visibility = View.GONE
            btnFollow.setOnClickListener {
                startActivity(android.content.Intent(this, ProfileInformation::class.java))
            }
        } else {
            // Check current follow status
            checkFollowStatus(btnFollow)
            
            btnFollow.setOnClickListener {
                toggleFollow(btnFollow)
            }
            btnMessage.setOnClickListener {
                showToast("Chat feature coming soon!")
            }
        }
    }

    private fun checkFollowStatus(button: com.google.android.material.button.MaterialButton) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection(Constants.COLLECTION_FOLLOWERS)
            .document(profileUserId!!)
            .collection("UserFollowers")
            .document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    setFollowingUI(button)
                } else {
                    setFollowUI(button)
                }
            }
    }

    private fun setFollowingUI(button: com.google.android.material.button.MaterialButton) {
        button.text = "Following"
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F2F4F7")))
        button.setTextColor(resources.getColor(R.color.assetgrey, null))
    }

    private fun setFollowUI(button: com.google.android.material.button.MaterialButton) {
        button.text = "Follow"
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resources.getColor(R.color.green, null)))
        button.setTextColor(android.graphics.Color.WHITE)
    }

    private fun toggleFollow(button: com.google.android.material.button.MaterialButton) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileRef = db.collection(Constants.COLLECTION_FOLLOWERS).document(profileUserId!!)
        val followerRef = profileRef.collection("UserFollowers").document(currentUserId)

        if (button.text == "Follow") {
            // Follow
            val data = hashMapOf<String, Any>(
                "timestamp" to FieldValue.serverTimestamp()
            )
            followerRef.set(data).addOnSuccessListener {
                // Update follower count in User document
                db.collection(Constants.COLLECTION_USERS).document(profileUserId!!)
                    .update("followers", FieldValue.increment(1))
            }
        } else {
            // Unfollow
            followerRef.delete().addOnSuccessListener {
                // Update follower count in User document
                db.collection(Constants.COLLECTION_USERS).document(profileUserId!!)
                    .update("followers", FieldValue.increment(-1))
            }
        }
    }

    private fun setupRecyclerViews() {
        // Collabs - Horizontal as requested
        rvCollabs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        collabAdapter = AdapterCollabProgress(collabList) { collab ->
            // Handle click if needed
        }
        rvCollabs.adapter = collabAdapter

        // Tasks - Vertical
        rvTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = AdapterTasksPriority(taskList, FirebaseAuth.getInstance().currentUser?.uid ?: "") { task ->
            // Handle request click if needed (though usually we don't send requests from profile view of others? or we do?)
        }
        rvTasks.adapter = taskAdapter
    }

    private fun loadProfileData() {
        db.collection(Constants.COLLECTION_USERS).document(profileUserId!!)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: "User"
                    val bio = doc.getString("bio") ?: ""
                    val imageUrl = doc.getString("profileImageUrl")
                    val followers = doc.getLong("followers") ?: 0L

                    tvProfileName.text = name
                    tvProfileBio.text = if (bio.isEmpty()) "Digital Creator & Brand Strategist." else bio
                    tvProfileFollowersCount.text = if (followers >= 1000) "${followers/1000}k" else followers.toString()

                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.profile_image).into(ivProfileImage)
                        } else {
                            try {
                                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivProfileImage.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                ivProfileImage.setImageResource(R.drawable.profile_image)
                            }
                        }
                    }
                }
            }
    }

    private fun loadCollabs() {
        db.collection(Constants.COLLECTION_COLLABS)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshots != null) {
                    collabList.clear()
                    var completedCount = 0
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    
                    for (doc in snapshots) {
                        val senderId = doc.getString("senderId") ?: ""
                        val receiverId = doc.getString("receiverId") ?: ""
                        val status = doc.getString("status") ?: ""
                        
                        // Check if profile user is involved
                        if (senderId == profileUserId || receiverId == profileUserId) {
                            
                            // Only add to list if it's an active/completed collab (not pending/declined)
                            if (status == "Accepted" || status == "Running" || status == "Completed") {
                                val senderName = doc.getString("senderName") ?: "User"
                                val receiverName = doc.getString("receiverName") ?: "User"

                                val displayName = if (senderId == currentUserId) {
                                    "You & $receiverName"
                                } else if (receiverId == currentUserId) {
                                    "$senderName & You"
                                } else {
                                    "$senderName & $receiverName"
                                }

                                collabList.add(ModelCollabProgress(
                                    id = doc.id,
                                    copProfileImg1 = doc.getString("senderImage") ?: "",
                                    copProfileImg2 = doc.getString("receiverImage") ?: "",
                                    copName = displayName,
                                    copDescription = "Status: $status",
                                    copProjectTitle = doc.getString("taskTitle") ?: "Project",
                                    copCategory = status,
                                    copProgress = doc.getLong("progress")?.toInt() ?: 0,
                                    senderId = senderId,
                                    receiverId = receiverId
                                ))
                                
                                if (status == "Completed") {
                                    completedCount++
                                }
                            }
                        }
                    }
                    collabAdapter.notifyDataSetChanged()
                    tvProfileCollabsCount.text = completedCount.toString()
                }
            }
    }

    private fun loadTasks() {
        db.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("userId", profileUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    taskList.clear()
                    for (doc in snapshots) {
                        val task = ModelTaskPriority(
                            id = doc.id,
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
                        taskList.add(task)
                    }
                    taskAdapter.notifyDataSetChanged()
                    tvProfileTasksCount.text = taskList.size.toString()
                }
            }
    }
}
