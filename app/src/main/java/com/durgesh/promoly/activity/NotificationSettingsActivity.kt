package com.durgesh.promoly.activity

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.SwitchCompat
import com.durgesh.promoly.R
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotifSettings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferenceManager = PreferenceManager(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        setupSwitches()
    }

    private fun setupSwitches() {
        val switches = mapOf(
            R.id.switchCollabRequests to PreferenceManager.KEY_NOTIF_COLLAB_REQUESTS,
            R.id.switchMessages to PreferenceManager.KEY_NOTIF_MESSAGES,
            R.id.switchCollabUpdates to PreferenceManager.KEY_NOTIF_COLLAB_UPDATES,
            R.id.switchTaskReminders to PreferenceManager.KEY_NOTIF_TASK_REMINDERS,
            R.id.switchDueDateAlerts to PreferenceManager.KEY_NOTIF_DUE_DATE,
            R.id.switchTaskSummary to PreferenceManager.KEY_NOTIF_COMPLETED_TASK,
            R.id.switchSecurityAlerts to PreferenceManager.KEY_NOTIF_SECURITY,
            R.id.switchProUpdates to PreferenceManager.KEY_NOTIF_PRO_UPDATES,
            R.id.switchMarketingEmails to PreferenceManager.KEY_NOTIF_MARKETING
        )

        for ((id, key) in switches) {
            val switch = findViewById<SwitchCompat>(id)
            switch.isChecked = preferenceManager.getNotificationSetting(key)
            
            switch.setOnCheckedChangeListener { _, isChecked ->
                preferenceManager.setNotificationSetting(key, isChecked)
                updateFirestoreSetting(key, isChecked)
            }
        }
    }

    private fun updateFirestoreSetting(key: String, value: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update(key, value)
            .addOnFailureListener {
                // Silently fail to maintain smooth UI
            }
    }
}
