package com.durgesh.promoly.adapter

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
        
        holder.tvName.text = item.copName
        holder.tvProjectTitle.text = item.copProjectTitle
        holder.tvDescription.text = item.copDescription
        holder.tvCategory.text = item.copCategory
        
        // Load Profile Images
        loadImage(holder.image1, item.copProfileImg1)
        loadImage(holder.image2, item.copProfileImg2)
        
        // Handling progress
        holder.tvProgressPercent.text = "${item.copProgress}%"
        holder.progressBar.progress = item.copProgress
    }

    private fun loadImage(imageView: ImageView, imageData: String) {
        if (imageData.isEmpty()) {
            imageView.setImageResource(R.drawable.profile_image)
            return
        }

        if (imageData.startsWith("http")) {
            Glide.with(imageView.context)
                .load(imageData)
                .placeholder(R.drawable.profile_image)
                .into(imageView)
        } else {
            try {
                val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.profile_image)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image1: ImageView = itemView.findViewById(R.id.ivProfile1)
        val image2: ImageView = itemView.findViewById(R.id.ivProfile2)
        val tvName: TextView = itemView.findViewById(R.id.tvCollabName)
        val tvProjectTitle: TextView = itemView.findViewById(R.id.tvProjectTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvCollabDescription)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.projectProgressBar)
        val tvProgressPercent: TextView = itemView.findViewById(R.id.tvProgressPercent)
    }
}
