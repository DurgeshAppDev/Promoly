package com.durgesh.promoly.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
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
import com.durgesh.promoly.adapter.AdapterChat
import com.durgesh.promoly.model.ModelChatMessage
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.FcmNotificationSender
import com.durgesh.promoly.util.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvUserName: TextView
    private lateinit var ivProfile: ImageView

    private lateinit var adapter: AdapterChat
    private val messageList = mutableListOf<ModelChatMessage>()
    
    private var receiverId: String? = null
    private var chatId: String? = null
    private var currentUserName: String = "Name"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        receiverId = intent.getStringExtra("receiverId")
        if (receiverId == null) {
            showToast("User not found")
            finish()
            return
        }

        val currentUserId = auth.currentUser?.uid ?: return
        chatId = if (currentUserId < receiverId!!) {
            "${currentUserId}_${receiverId}"
        } else {
            "${receiverId}_${currentUserId}"
        }

        initViews()
        loadReceiverInfo()
        loadCurrentUserInfo()
        setupRecyclerView()
        loadMessages()

        btnSend.setOnClickListener {
            val msgText = etMessage.text.toString().trim()
            if (msgText.isNotEmpty()) {
                sendMessage(msgText)
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etMessageChatRoom)
        btnSend = findViewById(R.id.btnSendMessage)
        btnBack = findViewById(R.id.btnBackChatRoom)
        tvUserName = findViewById(R.id.tvUserNameChatRoom)
        ivProfile = findViewById(R.id.ivProfileImageChatRoom)
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        adapter = AdapterChat(messageList, auth.currentUser?.uid ?: "")
        rvMessages.adapter = adapter
    }

    private fun loadReceiverInfo() {
        db.collection(Constants.COLLECTION_USERS).document(receiverId!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Name"
                    val imageUrl = doc.getString("profileImageUrl")
                    
                    tvUserName.text = name
                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.user).into(ivProfile)
                        } else {
                            try {
                                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivProfile.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                ivProfile.setImageResource(R.drawable.user)
                            }
                        }
                    }
                }
            }
    }

    private fun loadCurrentUserInfo() {
        db.collection(Constants.COLLECTION_USERS).document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "Name"
            }
    }

    private fun loadMessages() {
        db.collection(Constants.COLLECTION_CHATS)
            .document(chatId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatRoom", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messageList.clear()
                    for (doc in snapshots) {
                        val message = ModelChatMessage(
                            messageId = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                        messageList.add(message)
                    }
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        rvMessages.smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }
    }

    private fun sendMessage(text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        val messageId = db.collection(Constants.COLLECTION_CHATS).document(chatId!!)
            .collection("messages").document().id

        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "receiverId" to receiverId!!,
            "message" to text,
            "timestamp" to System.currentTimeMillis()
        )

        etMessage.setText("")

        db.collection(Constants.COLLECTION_CHATS).document(chatId!!)
            .collection("messages").document(messageId)
            .set(messageData)
            .addOnSuccessListener {
                // Send notification to the receiver
                FcmNotificationSender.sendNotification(
                    context = this,
                    receiverId = receiverId!!,
                    title = currentUserName,
                    message = text,
                    type = "chat"
                )
            }
            .addOnFailureListener { e ->
                showToast("Failed to send message: ${e.message}")
            }
    }
}
