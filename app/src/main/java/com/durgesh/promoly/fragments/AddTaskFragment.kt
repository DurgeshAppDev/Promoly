package com.durgesh.promoly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddTaskFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etProjectTitle: EditText
    private lateinit var etCategory: EditText
    private lateinit var etPlatform: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var etProjectDescription: EditText
    private lateinit var chipGroupSkills: ChipGroup
    private lateinit var etBudgetRange: EditText
    private lateinit var etTimeline: EditText
    private lateinit var btnAddSkill: MaterialButton
    private lateinit var btnPostCollab: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_task, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind views
        etProjectTitle = view.findViewById(R.id.etProjectTitle)
        etCategory = view.findViewById(R.id.etCategory)
        etPlatform = view.findViewById(R.id.etPlatform)
        spinnerPriority = view.findViewById(R.id.spinnerPriority)
        etProjectDescription = view.findViewById(R.id.etProjectDescription)
        chipGroupSkills = view.findViewById(R.id.chipGroupSkills)
        etBudgetRange = view.findViewById(R.id.etBudgetRange)
        etTimeline = view.findViewById(R.id.etTimeline)
        btnAddSkill = view.findViewById(R.id.btnAddSkill)
        btnPostCollab = view.findViewById(R.id.btnPostCollab)

        setupPrioritySpinner()

        btnAddSkill.setOnClickListener {
            showAddSkillDialog()
        }

        btnPostCollab.setOnClickListener {
            saveTaskToFirestore()
        }

        return view
    }

    private fun setupPrioritySpinner() {
        val priorities = arrayOf("High Priority", "Medium Priority", "Low Priority")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
        spinnerPriority.setSelection(1) // Default to Medium
    }

    private fun showAddSkillDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_skill, null)
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        builder.setView(dialogView)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etSkillInput = dialogView.findViewById<EditText>(R.id.etSkillInput)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelSkill)
        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btnAddSkillConfirm)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {
            val skill = etSkillInput.text.toString().trim()
            if (skill.isNotEmpty()) {
                addSkillChip(skill)
                dialog.dismiss()
            } else {
                etSkillInput.error = "Please enter a skill"
            }
        }

        dialog.show()
    }

    private fun addSkillChip(skillName: String) {
        val chip = Chip(requireContext())
        chip.text = skillName
        chip.isCloseIconVisible = true
        chip.setChipBackgroundColorResource(R.color.lightgreen)
        chip.setTextColor(resources.getColor(R.color.green, null))
        chip.closeIconTint = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.green, null))

        chip.setOnCloseIconClickListener {
            chipGroupSkills.removeView(chip)
        }

        // Add chip before the "Add Skill" button
        val index = chipGroupSkills.indexOfChild(btnAddSkill)
        chipGroupSkills.addView(chip, index)
    }

    private fun saveTaskToFirestore() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Fetch User Data first to attach to the task
        db.collection(Constants.COLLECTION_USERS).document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "Name"
                val userProfileImage = userDoc.getString("profileImageUrl") ?: ""

                val title = etProjectTitle.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val platform = etPlatform.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()
                val description = etProjectDescription.text.toString().trim()
                val budget = etBudgetRange.text.toString().trim()
                val timeline = etTimeline.text.toString().trim()

                if (title.isEmpty()) {
                    etProjectTitle.error = "Title is required"
                    return@addOnSuccessListener
                }

                // Collect skills from ChipGroup
                val selectedSkills = mutableListOf<String>()
                for (i in 0 until chipGroupSkills.childCount) {
                    val child = chipGroupSkills.getChildAt(i)
                    if (child is Chip) {
                        selectedSkills.add(child.text.toString())
                    }
                }

                // Generate a unique ID for the task
                val taskRef = db.collection(Constants.COLLECTION_TASKS).document()
                val taskId = taskRef.id

                val taskMap = hashMapOf(
                    "id" to taskId,
                    "userId" to currentUserId,
                    "userName" to userName,
                    "userProfileImage" to userProfileImage,
                    "projectTitle" to title,
                    "category" to category,
                    "platform" to platform,
                    "priority" to priority,
                    "description" to description,
                    "skills" to selectedSkills,
                    "budget" to budget,
                    "timeline" to timeline,
                    "status" to "Active", // Default status
                    "progress" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                showToast("Posting task...")
                btnPostCollab.isEnabled = false

                taskRef.set(taskMap)
                    .addOnSuccessListener {
                        showToast("Task posted successfully!")
                        // Navigate back
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error posting task: ${e.message}")
                        btnPostCollab.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                showToast("Failed to fetch user info: ${e.message}")
            }
    }
}
