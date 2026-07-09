package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterFollowersList
import com.durgesh.promoly.model.ModelAllFollowersList
import com.durgesh.promoly.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class All_Followers_Activity : AppCompatActivity() {
    private lateinit var rvAllFollowersList: RecyclerView
    private lateinit var adapter: AdapterFollowersList
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_followers)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotifSettings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid

        rvAllFollowersList = findViewById(R.id.rvAllFollowers)
        rvAllFollowersList.layoutManager = LinearLayoutManager(this)
        adapter = AdapterFollowersList(emptyList()) { follower ->
            val intent = Intent(this, ViewProfile::class.java)
            intent.putExtra("userId", follower.userId)
            startActivity(intent)
        }
        rvAllFollowersList.adapter = adapter

        loadFollowers()
    }

    private fun loadFollowers() {
        if (userId == null) return

        db.collection(Constants.COLLECTION_FOLLOWERS)
            .document(userId!!)
            .collection("UserFollowers")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val followersList = mutableListOf<ModelAllFollowersList>()
                    var processedCount = 0
                    val totalFollowers = snapshots.size()

                    for (doc in snapshots.documents) {
                        val followerId = doc.id
                        db.collection(Constants.COLLECTION_USERS).document(followerId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val name = userDoc.getString("name") ?: ""
                                    val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                                    followersList.add(
                                        ModelAllFollowersList(
                                            followerId,
                                            name,
                                            profileImageUrl
                                        )
                                    )
                                }
                                processedCount++
                                if (processedCount == totalFollowers) {
                                    adapter.updateList(followersList)
                                }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == totalFollowers) {
                                    adapter.updateList(followersList)
                                }
                            }
                    }
                } else {
                    adapter.updateList(emptyList())
                }
            }
    }
}