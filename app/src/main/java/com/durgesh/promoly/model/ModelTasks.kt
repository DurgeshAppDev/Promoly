package com.durgesh.promoly.model

data class ModelTasks(
    val id: String = "",
    val taskImage: String = "",
    val taskTitle: String = "",
    val taskDescription: String = "",
    val taskStatus: String = "Active",
    val type: String = "task" // "task" or "collab"
)
