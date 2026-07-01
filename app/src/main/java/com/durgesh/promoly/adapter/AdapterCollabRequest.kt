package com.durgesh.promoly.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.durgesh.promoly.R
import com.durgesh.promoly.model.ModelCollabRequest
import com.durgesh.promoly.util.showToast

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.FcmNotificationSender

class AdapterCollabRequest(
    private val list: MutableList<ModelCollabRequest>,
    private val onActionComplete: () -> Unit
) : RecyclerView.Adapter<AdapterCollabRequest.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collab_request_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvdescription.text = item.coDescription
        holder.tvname.text = item.coName
        
        val onProfileClick = View.OnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, com.durgesh.promoly.activity.ViewProfile::class.java)
            intent.putExtra("userId", item.senderId)
            context.startActivity(intent)
        }
        
        holder.tvname.setOnClickListener(onProfileClick)
        holder.image.setOnClickListener(onProfileClick)
        
        // Handle Image loading (Base64 or URL)
        if (item.coProfileImg.isNotEmpty()) {
            if (item.coProfileImg.startsWith("http")) {
                Glide.with(holder.itemView.context).load(item.coProfileImg).placeholder(R.drawable.user).into(holder.image)
            } else {
                try {
                    val imageBytes = Base64.decode(item.coProfileImg, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.image.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    holder.image.setImageResource(R.drawable.user)
                }
            }
        } else {
            holder.image.setImageResource(R.drawable.user)
        }

        holder.tvAccept.setOnClickListener {
            updateRequestStatus(item.id, "Accepted", holder, item)
        }
        holder.tvDecline.setOnClickListener {
            updateRequestStatus(item.id, "Declined", holder, item)
        }
    }

    private fun updateRequestStatus(requestId: String, newStatus: String, holder: ViewHolder, item: ModelCollabRequest) {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        holder.tvAccept.isEnabled = false
        holder.tvDecline.isEnabled = false

        if (newStatus == "Accepted") {
            // 1. Check if task is still available
            db.collection(Constants.COLLECTION_TASKS).document(item.taskId).get()
                .addOnSuccessListener { taskDoc ->
                    val taskStatus = taskDoc.getString("status") ?: "Active"
                    if (taskStatus != "Active") {
                        holder.itemView.context.showToast("This task is no longer available.")
                        holder.tvAccept.isEnabled = true
                        holder.tvDecline.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // 2. Fetch acceptor (current user) info
                    db.collection(Constants.COLLECTION_USERS).document(currentUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val receiverName = userDoc.getString("name") ?: "Name"
                            val receiverImage = userDoc.getString("profileImageUrl") ?: ""

                            val updates = hashMapOf(
                                "status" to "Accepted",
                                "receiverName" to receiverName,
                                "receiverImage" to receiverImage,
                                "acceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )

                            // 3. Accept this specific request
                            db.collection(Constants.COLLECTION_COLLABS).document(requestId)
                                .update(updates as Map<String, Any>)
                                .addOnSuccessListener {
                                    // 4. Mark Task as Accepted (Removes it from "All Tasks" for everyone)
                                    db.collection(Constants.COLLECTION_TASKS).document(item.taskId)
                                        .update("status", "Accepted")

                                    val context = holder.itemView.context
                                    context.showToast("Collaboration Accepted!")

                                    // 5. Notify the lucky requester
                                    FcmNotificationSender.sendNotification(
                                        context = context,
                                        receiverId = item.senderId,
                                        title = "Collaboration Accepted!",
                                        message = "$receiverName has accepted your request for \"${item.coDescription}\"",
                                        type = "collab_accept"
                                    )

                                    // 6. HANDLE OTHERS: Notify all other pending requesters for this task
                                    notifyOtherRequesters(context, item.taskId, requestId, receiverName)

                                    onActionComplete()
                                }
                        }
                }
        } else {
            db.collection(Constants.COLLECTION_COLLABS).document(requestId)
                .update("status", newStatus)
                .addOnSuccessListener {
                    holder.itemView.context.showToast("Request $newStatus")
                    onActionComplete()
                }
        }
    }

    private fun notifyOtherRequesters(context: android.content.Context, taskId: String, acceptedRequestId: String, acceptorName: String) {
        val db = FirebaseFirestore.getInstance()
        
        // Find all other pending requests for this task
        db.collection(Constants.COLLECTION_COLLABS)
            .whereEqualTo("taskId", taskId)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    if (doc.id == acceptedRequestId) continue // Skip the one we just accepted

                    val otherSenderId = doc.getString("senderId") ?: continue
                    val taskTitle = doc.getString("taskTitle") ?: "Project"

                    // Notify them
                    FcmNotificationSender.sendNotification(
                        context = context,
                        receiverId = otherSenderId,
                        title = "Task Update",
                        message = "$acceptorName has accepted another user for \"$taskTitle\"",
                        type = "collab_denied_others"
                    )

                    // Optionally update their status to "Closed" or "Declined"
                    db.collection(Constants.COLLECTION_COLLABS).document(doc.id)
                        .update("status", "Closed")
                }
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