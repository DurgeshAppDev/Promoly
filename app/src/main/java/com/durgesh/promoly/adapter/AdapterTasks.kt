package com.durgesh.promoly.adapter

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.activity.ViewProfile
import com.durgesh.promoly.model.ModelTasks

class AdapterTasks(private val list: List<ModelTasks>): RecyclerView.Adapter<AdapterTasks.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campaign_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        holder.tvTitle.text = item.taskTitle
        holder.tvDescription.text = item.taskDescription
        holder.tvStatus.text = item.taskStatus

        holder.image.setOnClickListener {
            if (item.userId.isNotEmpty()) {
                val context = holder.itemView.context
                val intent = Intent(context, ViewProfile::class.java)
                intent.putExtra("userId", item.userId)
                context.startActivity(intent)
            }
        }

        // Handle Image loading (Base64 or URL)
        if (item.taskImage.isNotEmpty()) {
            if (item.taskImage.startsWith("http")) {
                Glide.with(holder.itemView.context)
                    .load(item.taskImage)
                    .placeholder(R.drawable.user)
                    .into(holder.image)
            } else {
                try {
                    val imageBytes = Base64.decode(item.taskImage, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.image.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    holder.image.setImageResource(R.drawable.user)
                }
            }
        } else {
            holder.image.setImageResource(R.drawable.user)
        }
        
        // Status color coding
        if (item.taskStatus == "Active" || item.taskStatus == "Running" || item.taskStatus == "Accepted") {
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.green))
        } else {
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.grey))
        }
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
