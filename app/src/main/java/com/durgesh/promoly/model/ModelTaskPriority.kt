package com.durgesh.promoly.model

data class ModelTaskPriority(
    val id: String = "",
    val priority: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val timeline: String = "",
    val budget: String = "",
    val progress: Int = 0,
    val skills: List<String> = emptyList()
)
