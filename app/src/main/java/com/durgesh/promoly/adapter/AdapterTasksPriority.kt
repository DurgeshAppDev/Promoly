package com.durgesh.promoly.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelTaskPriority
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.content.res.ColorStateList
import androidx.core.widget.TextViewCompat

class AdapterTasksPriority(
    private val list: List<ModelTaskPriority>,
    private val currentUserId: String,
    private val onRequestClick: (ModelTaskPriority) -> Unit
) : RecyclerView.Adapter<AdapterTasksPriority.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_priority_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        // User Info
        holder.tvUserName.text = item.userName

        val onProfileClick = View.OnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, com.durgesh.promoly.activity.ViewProfile::class.java)
            intent.putExtra("userId", item.userId)
            context.startActivity(intent)
        }

        holder.tvUserName.setOnClickListener(onProfileClick)
        holder.ivProfileImage.setOnClickListener(onProfileClick)

        // Hide/Show Send Request button: only show if it's NOT the current user's task
        if (item.userId == currentUserId) {
            holder.btnSendRequest.visibility = View.GONE
        } else {
            holder.btnSendRequest.visibility = View.VISIBLE
            holder.btnSendRequest.setOnClickListener {
                onRequestClick(item)
            }
        }
        if (item.userProfileImage.isNotEmpty()) {
            if (item.userProfileImage.startsWith("http")) {
                Glide.with(holder.itemView.context).load(item.userProfileImage).into(holder.ivProfileImage)
            } else {
                try {
                    val imageBytes = Base64.decode(item.userProfileImage, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.ivProfileImage.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    holder.ivProfileImage.setImageResource(R.drawable.user)
                }
            }
        } else {
            holder.ivProfileImage.setImageResource(R.drawable.user)
        }

        holder.tvPriority.text = item.priority
        
        // Color coding for priority
        when (item.priority) {
            "High Priority" -> {
                holder.tvPriority.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                holder.tvPriority.setTextColor(Color.parseColor("#991B1B"))
            }
            "Medium Priority" -> {
                holder.tvPriority.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                holder.tvPriority.setTextColor(Color.parseColor("#92400E"))
            }
            "Low Priority" -> {
                holder.tvPriority.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                holder.tvPriority.setTextColor(Color.parseColor("#075985"))
            }
        }

        holder.tvCategory.text = "Category: ${item.category}"
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.tvTimeline.text = item.timeline
        holder.tvBudget.text = if (item.budget.startsWith("Budget")) item.budget else "Budget: ${item.budget}"
        holder.tvStatus.text = "Status: ${item.status}"
        holder.pbProgress.progress = item.progress

        // Dynamic Chips for Skills
        holder.cgSkills.removeAllViews()
        if (item.skills.isEmpty()) {
            holder.cgSkills.visibility = View.GONE
        } else {
            holder.cgSkills.visibility = View.VISIBLE
            item.skills.forEach { skill ->
                val chip = Chip(holder.itemView.context)
                chip.text = skill
                chip.setChipBackgroundColorResource(R.color.lightgreen)
                chip.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
                chip.setTextAppearance(android.R.style.TextAppearance_Small)
                chip.chipStrokeWidth = 0f
                holder.cgSkills.addView(chip)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfileImage: ImageView = itemView.findViewById(R.id.ivProfileImage)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvTimeline: TextView = itemView.findViewById(R.id.tvTimeline)
        val tvBudget: TextView = itemView.findViewById(R.id.tvBudget)
        val cgSkills: ChipGroup = itemView.findViewById(R.id.cgSkills)
        val pbProgress: LinearProgressIndicator = itemView.findViewById(R.id.pbTaskProgress)
        val btnSendRequest: View = itemView.findViewById(R.id.btnSendRequest)
    }
}
