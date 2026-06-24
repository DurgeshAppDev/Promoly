package com.durgesh.promoly.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FcmNotificationSender {

    // IMPORTANT: Storing Server Key in app is not secure for production.
    // Use Firebase Cloud Functions for production apps.
    private const val SERVER_KEY = "BDdg3EDNXLYjV0KJiengI3QKNeNfD1mN-vgk9HdiaA5iWSTd3X9fNHExEzZwwY587DAHQlZJdrWol1cfLg2GJj4"
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    fun sendNotification(receiverId: String, title: String, message: String) {
        val db = FirebaseFirestore.getInstance()
        
        db.collection(Constants.COLLECTION_USERS).document(receiverId).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    executeSend(fcmToken, title, message)
                } else {
                    Log.w("FcmSender", "Receiver has no FCM token")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FcmSender", "Error fetching receiver token", e)
            }
    }

    private fun executeSend(token: String, title: String, message: String) {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val json = JSONObject()
        val notification = JSONObject()
        notification.put("title", title)
        notification.put("body", message)
        
        json.put("to", token)
        json.put("notification", notification)

        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(FCM_URL)
            .post(requestBody)
            .addHeader("Authorization", "key=$SERVER_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FcmSender", "Failed to send notification", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FcmSender", "Notification sent: ${response.code}")
                response.close()
            }
        })
    }
}
