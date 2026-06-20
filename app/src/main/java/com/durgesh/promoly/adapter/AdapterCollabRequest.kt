package com.durgesh.promoly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R


import com.durgesh.promoly.model.ModelCollabRequest


class AdapterCollabRequest(private val list: List<ModelCollabRequest>) : RecyclerView.Adapter<AdapterCollabRequest.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collab_request_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvdescription.text = item.coDescription
        holder.tvname.text = item.coName
        holder.image.setImageResource(item.coProfileImg)

        holder.tvAccept.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Accepted request of ${item.coName}", Toast.LENGTH_SHORT).show()
        }
        holder.tvDecline.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Declined request of ${item.coName}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.ivProfileImagecollab)
        val tvname: TextView = itemView.findViewById(R.id.tvCollabName)
        val tvdescription: TextView = itemView.findViewById(R.id.tvCollabMessage)
        val tvAccept: AppCompatButton = itemView.findViewById(R.id.btnAccept)
        val tvDecline: AppCompatButton = itemView.findViewById(R.id.btnDecline)
    }
}