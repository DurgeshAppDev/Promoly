package com.durgesh.promoly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelTaskPriority
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

class AdapterTasksPriority(private val list: List<ModelTaskPriority>) : RecyclerView.Adapter<AdapterTasksPriority.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_priority_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvPriority.text = item.priority
        holder.tvCategory.text = item.category
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.tvTimeline.text = item.timeline
        holder.tvBudget.text = item.budget
        holder.pbProgress.progress = item.progress

        // Dynamic Chips for Skills
        holder.cgSkills.removeAllViews()
        item.skills.forEach { skill ->
            val chip = Chip(holder.itemView.context)
            chip.text = skill
            chip.setChipBackgroundColorResource(R.color.lightgreen)
            chip.setTextColor(holder.itemView.context.getColor(R.color.green))
            chip.setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Caption)
            chip.chipStrokeWidth = 0f
            holder.cgSkills.addView(chip)
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvTimeline: TextView = itemView.findViewById(R.id.tvTimeline)
        val tvBudget: TextView = itemView.findViewById(R.id.tvBudget)
        val cgSkills: ChipGroup = itemView.findViewById(R.id.cgSkills)
        val pbProgress: LinearProgressIndicator = itemView.findViewById(R.id.pbTaskProgress)
    }
}
