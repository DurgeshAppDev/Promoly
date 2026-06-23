package com.durgesh.promoly.model

data class ModelCollabRequest(
    val id: String = "",
    val coProfileImg: String = "", // Changed to String for Firestore support
    val coName: String = "",
    val coDescription: String = "",
    val senderId: String = "",
    val taskId: String = "",
    val status: String = "Pending"
)
