package com.durgesh.promoly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterCollabRequest
import com.durgesh.promoly.adapter.AdapterTasks
import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.model.ModelTasks
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        val profileImage = view.findViewById<ImageView>(R.id.ivProfileImageofHome)
        val searchEditText = view.findViewById<EditText>(R.id.etSearchHome)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearchHome)
        val rvCollabRequests = view.findViewById<RecyclerView>(R.id.rvCollabRequests)
        val rvTodaysTasks = view.findViewById<RecyclerView>(R.id.rvTodaysTasks)

        profileImage.setOnClickListener {
            //go to profile
            showToast("profile image is clicked")
        }

        // Dummy data for Tasks
        val tasksList = listOf(
            ModelTasks("1", R.drawable.ic_tasks, "Post Review", "Review for XYZ Brand on Instagram", "Pending"),
            ModelTasks("2", R.drawable.ic_tasks, "Video Editing", "Edit the collab video with Alex", "In Progress"),
            ModelTasks("3", R.drawable.ic_tasks, "Campaign Pitch", "Submit pitch for New Winter Collection", "Completed"),
            ModelTasks("4", R.drawable.ic_tasks, "Client Call", "Meeting with Brand Manager at 5 PM", "Scheduled"),
            ModelTasks("5", R.drawable.ic_tasks, "Content Calendar", "Update next month's post schedule", "Pending")
        )

        rvTodaysTasks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvTodaysTasks.adapter = AdapterTasks(tasksList)

        // Dummy data for Collab Requests
        val collabList = listOf(
            ModelCollabRequest("1", R.drawable.profile_image, "Sarah (Designer)", "Wants to collaborate on UI Project"),
            ModelCollabRequest("2", R.drawable.profile_image, "Alex (Marketing)", "Brand Campaign Pitch"),
            ModelCollabRequest("3", R.drawable.profile_image, "John (Dev)", "E-commerce App Collab"),
            ModelCollabRequest("4", R.drawable.profile_image, "Emma (Writer)", "Blog Content Strategy"),
            ModelCollabRequest("5", R.drawable.profile_image, "Mike (Photo)", "Product Photoshoot Request")
        )

        rvCollabRequests.layoutManager = LinearLayoutManager(requireContext())
        rvCollabRequests.adapter = AdapterCollabRequest(collabList)


        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            if (searchText.isNotEmpty()) {
                FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS)
                    .whereGreaterThanOrEqualTo("username", searchText)
                    .whereLessThanOrEqualTo("username", searchText + "\uf8ff")
                    .get()
                    .addOnSuccessListener { documents ->
                        // Update your adapter with documents.toObjects(User::class.java)
                    }
                    .addOnFailureListener { e ->
                        showToast("Error: ${e.message}")
                    }
            }
        }

        return view
    }
}