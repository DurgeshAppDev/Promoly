package com.durgesh.promoly.model

data class ModelCollabRequest(
    val id: String = "",
    val coProfileImg: Int = 0,
    val coName: String = "",
    val coDescription: String = "",
    val accept: String ="",
    val decline: String ="",
)
