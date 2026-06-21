package com.durgesh.promoly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterTasksPriority
import com.durgesh.promoly.model.ModelTaskPriority

class TasksFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTaskPriorities)

        val taskList = listOf(
            ModelTaskPriority(
                "1", "High Priority", "Fashion", 
                "Finalize Contract", "Designing the high-fidelity wireframes for the Acme dashboard.", 
                "2-3 Weeks", "$ 500 - 1000", 75, 
                listOf("Legal", "Contract", "Drafting")
            ),
            ModelTaskPriority(
                "2", "Medium Priority", "Tech", 
                "Edit YouTube Video", "Executing the social media campaign strategy for the Morning Workflow series.", 
                "1 Week", "$ 200 - 400", 30, 
                listOf("Editing", "Video", "Creative")
            ),
            ModelTaskPriority(
                "3", "Low Priority", "Business", 
                "Respond to Emails", "Check and reply to all collaboration inquiries received today.", 
                "2 Days", "N/A", 100, 
                listOf("Admin", "Communication")
            ),
            ModelTaskPriority(
                "4", "High Priority", "Lifestyle", 
                "Client Meeting", "Project sync with Fashion Brand for upcoming winter collection launch.", 
                "Today", "N/A", 0, 
                listOf("Meeting", "Strategy", "Fashion")
            )
        )

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = AdapterTasksPriority(taskList)

        return view
    }
}