package com.durgesh.promoly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelCollabProgress
import com.google.android.material.progressindicator.LinearProgressIndicator

class AdapterCollabProgress(private val list: List<ModelCollabProgress>) : RecyclerView.Adapter<AdapterCollabProgress.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collab_progress_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.image.setImageResource(item.copProfileImg)
        holder.tvName.text = item.copName
        holder.tvProjectTitle.text = item.copProjectTitle
        holder.tvDescription.text = item.copDescription
        
        // Handling progress
        holder.tvProgressPercent.text = "${item.copProgress}%"
        try {
            holder.progressBar.progress = item.copProgress.toInt()
        } catch (e: Exception) {
            holder.progressBar.progress = 0
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.ivProfileImagecollab)
        val tvName: TextView = itemView.findViewById(R.id.tvCollabName)
        val tvProjectTitle: TextView = itemView.findViewById(R.id.tvProjectTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvCollabDescription)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.projectProgressBar)
        val tvProgressPercent: TextView = itemView.findViewById(R.id.tvProgressPercent)
    }
}
