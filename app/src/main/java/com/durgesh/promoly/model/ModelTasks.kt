package com.durgesh.promoly.model

import androidx.appcompat.widget.DialogTitle
import com.google.rpc.Status

data class ModelTasks(
    val id: String = "",
    val taskImage: Int = 0,
    val taskTitle: String = "",
    val taskDescription: String = "",
    val taskStatus: String = "pending",
)
