package com.durgesh.promoly.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.durgesh.promoly.R
import com.durgesh.promoly.activity.ChatRoomActivity
import com.durgesh.promoly.activity.HomeActivity
import com.durgesh.promoly.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
        updateTokenInFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val type = data["type"] ?: "general"
        val senderId = data["senderId"] ?: ""
        val title = data["title"] ?: remoteMessage.notification?.title ?: "New Notification"
        val message = data["message"] ?: remoteMessage.notification?.body ?: ""

        showNotification(title, message, type, senderId)
    }

    private fun updateTokenInFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLLECTION_USERS).document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token updated in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error updating token", e)
            }
    }

    private fun showNotification(title: String, message: String, type: String, senderId: String) {
        val channelId = if (type == "chat") "chat_messages" else "collab_requests"
        val channelName = if (type == "chat") "Chat Messages" else "Collaboration Requests"
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = if (type == "chat" && senderId.isNotEmpty()) {
            Intent(this, ChatRoomActivity::class.java).apply {
                putExtra("receiverId", senderId)
            }
        } else {
            Intent(this, HomeActivity::class.java)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
