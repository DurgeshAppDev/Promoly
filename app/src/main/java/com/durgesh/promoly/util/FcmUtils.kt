package com.durgesh.promoly.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmUtils {

    fun updateFcmToken() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                saveTokenToFirestore(userId, token)
            } else {
                Log.w("FcmUtils", "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    private fun saveTokenToFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLLECTION_USERS).document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FcmUtils", "FCM Token updated successfully")
            }
            .addOnFailureListener { e ->
                // If document update fails (e.g. field doesn't exist), try to merge
                db.collection(Constants.COLLECTION_USERS).document(userId)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("FcmUtils", "FCM Token merged successfully")
                    }
                    .addOnFailureListener {
                        Log.e("FcmUtils", "Error updating FCM token", it)
                    }
            }
    }

    fun deleteFcmToken() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection(Constants.COLLECTION_USERS).document(userId)
            .update("fcmToken", "")
            .addOnSuccessListener {
                Log.d("FcmUtils", "FCM Token cleared from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FcmUtils", "Error clearing FCM token", e)
            }
    }
}
