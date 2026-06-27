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
import com.durgesh.promoly.model.ModelUserSearch

class AdapterUserSearch(
    private var list: List<ModelUserSearch>,
    private val onItemClick: (ModelUserSearch) -> Unit
) : RecyclerView.Adapter<AdapterUserSearch.ViewHolder>() {

    fun updateList(newList: List<ModelUserSearch>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]
        holder.tvName.text = user.name
        
        if (user.profileImageUrl.isNotEmpty()) {
            if (user.profileImageUrl.startsWith("http")) {
                Glide.with(holder.itemView.context).load(user.profileImageUrl).placeholder(R.drawable.user).into(holder.ivProfile)
            } else {
                try {
                    val imageBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
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
            onItemClick(user)
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivSearchUserImage)
        val tvName: TextView = itemView.findViewById(R.id.tvSearchUserName)
    }
}