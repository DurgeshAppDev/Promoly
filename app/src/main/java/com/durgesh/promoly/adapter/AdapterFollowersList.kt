package com.durgesh.promoly.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelAllFollowersList
import com.google.android.material.imageview.ShapeableImageView


class AdapterFollowersList(
    private var list: List<ModelAllFollowersList>,
    private val onItemClick: ((ModelAllFollowersList) -> Unit)? = null
) : RecyclerView.Adapter<AdapterFollowersList.ViewHolder>() {

    fun updateList(newList: List<ModelAllFollowersList>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.all_followers_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]
        holder.tvName.text = item.name

        if (item.profileImageUrl.isNotEmpty()) {
            if (item.profileImageUrl.startsWith("http")) {
                Glide.with(holder.itemView.context)
                    .load(item.profileImageUrl)
                    .placeholder(R.drawable.user)
                    .into(holder.ivProfile)
            } else {
                try {
                    val imageBytes = Base64.decode(item.profileImageUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.ivProfile.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    holder.ivProfile.setImageResource(R.drawable.user)
                }
            }
        } else {
            holder.ivProfile.setImageResource(R.drawable.user)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ShapeableImageView = view.findViewById(R.id.ivProfileImageFollower)
        val tvName: TextView = view.findViewById(R.id.tvCollabName)
    }
}
