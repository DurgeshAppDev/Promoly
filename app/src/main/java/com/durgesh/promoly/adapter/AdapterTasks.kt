package com.durgesh.promoly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelTasks

class AdapterTasks(val list: List<ModelTasks>): RecyclerView.Adapter<AdapterTasks.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campaign_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.image.setImageResource(item.taskImage)
        holder.tvTitle.text = item.taskTitle
        holder.tvDescription.text = item.taskDescription
        holder.tvStatus.text = item.taskStatus
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.ivCampaignIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvCampaignTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvCampaignDescription)
        val tvStatus: TextView = itemView.findViewById(R.id.tvCampaignStatus)
    }
}
