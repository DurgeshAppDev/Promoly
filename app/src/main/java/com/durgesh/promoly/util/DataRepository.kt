package com.durgesh.promoly.util

import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.model.ModelTasks

object DataRepository {
    val cachedTasks = mutableListOf<ModelTasks>()
    val cachedCollabRequests = mutableListOf<ModelCollabRequest>()
    
    // Flag to check if initial data is loaded
    var isDataLoaded = false
}
