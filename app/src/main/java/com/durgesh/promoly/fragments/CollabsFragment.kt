package com.durgesh.promoly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterCollabProgress
import com.durgesh.promoly.model.ModelCollabProgress

class CollabsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_collabs, container, false)

        val rvCollabProgress = view.findViewById<RecyclerView>(R.id.rvCollabRequests)

        // Dummy data for Collab Progress
        val progressList = listOf(
            ModelCollabProgress("1", R.drawable.profile_image, "Sarah (Designer)", "Designing the high-fidelity wireframes for the dashboard.", "UI/UX Redesign", "65"),
            ModelCollabProgress("2", R.drawable.profile_image, "Alex (Marketing)", "Executing the social media campaign strategy.", "Brand Campaign", "40"),
            ModelCollabProgress("3", R.drawable.profile_image, "John (Dev)", "Integrating the Firebase backend services.", "Mobile App Dev", "85"),
            ModelCollabProgress("4", R.drawable.profile_image, "Emma (Writer)", "Drafting the project documentation and user guides.", "Content Creation", "20"),
            ModelCollabProgress("5", R.drawable.profile_image, "Mike (Photo)", "Editing the promotional video for launch.", "Video Production", "55")
        )

        rvCollabProgress.layoutManager = LinearLayoutManager(requireContext())
        rvCollabProgress.adapter = AdapterCollabProgress(progressList)

        return view
    }
}