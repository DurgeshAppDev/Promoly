package com.durgesh.promoly.model

data class ModelTaskPriority(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val priority: String = "Medium Priority",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val timeline: String = "",
    val budget: String = "",
    val progress: Int = 0,
    val status: String = "Active",
    val skills: List<String> = emptyList(),
    val createdAt: Long = 0
)
