package com.durgesh.promoly.util

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FcmNotificationSender {

    private const val BASE_URL = "https://fcm.googleapis.com/v1/projects/"
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

    fun sendNotification(context: Context, receiverId: String, title: String, message: String, type: String = "general") {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        
        db.collection(Constants.COLLECTION_USERS).document(receiverId).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                
                val isEnabled = when (type) {
                    "collab_request" -> document.getBoolean(PreferenceManager.KEY_NOTIF_COLLAB_REQUESTS) ?: true
                    "chat" -> document.getBoolean(PreferenceManager.KEY_NOTIF_MESSAGES) ?: true
                    "collab_accept" -> document.getBoolean(PreferenceManager.KEY_NOTIF_COLLAB_UPDATES) ?: true
                    else -> true
                }

                if (!isEnabled) return@addOnSuccessListener

                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            executeSend(context, fcmToken, title, message, type, currentUserId)
                        } catch (e: Exception) {
                            Log.e("FcmSender", "Error: ${e.message}")
                        }
                    }
                }
            }
    }

    private suspend fun executeSend(context: Context, token: String, title: String, message: String, type: String, senderId: String) {
        val accessToken = withContext(Dispatchers.IO) { getAccessToken(context) } ?: return
        val projectId = withContext(Dispatchers.IO) { getProjectId(context) } ?: return
        val url = "$BASE_URL$projectId/messages:send"
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val json = JSONObject()
        val messageObj = JSONObject()
        val notificationObj = JSONObject()
        val dataObj = JSONObject()
        val androidObj = JSONObject()
        val androidNotificationObj = JSONObject()
        
        notificationObj.put("title", title)
        notificationObj.put("body", message)
        
        dataObj.put("type", type)
        dataObj.put("senderId", senderId)
        dataObj.put("title", title)
        dataObj.put("message", message)
        
        androidNotificationObj.put("channel_id", if (type == "chat") "chat_messages" else "collab_requests")
        androidObj.put("notification", androidNotificationObj)
        androidObj.put("priority", "high")
        
        messageObj.put("token", token)
        messageObj.put("notification", notificationObj)
        messageObj.put("data", dataObj)
        messageObj.put("android", androidObj)
        json.put("message", messageObj)

        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun getAccessToken(context: Context): String? {
        return try {
            val stream = context.assets.open("service-account.json")
            val credentials = GoogleCredentials.fromStream(stream).createScoped(listOf(SCOPE))
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            null
        }
    }

    private fun getProjectId(context: Context): String? {
        return try {
            val stream = context.assets.open("service-account.json")
            val jsonString = stream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("project_id")
        } catch (e: Exception) {
            null
        }
    }
}
