package com.durgesh.promoly.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PromolyPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_PROFESSION = "userProfession"
        private const val KEY_USER_BIO = "userBio"
        private const val KEY_USER_IMAGE = "userImage"
        private const val KEY_USER_FOLLOWERS = "userFollowers"
        private const val KEY_TASKS_COUNT = "tasksCount"
        private const val KEY_COLLABS_COUNT = "collabsCount"
        
        const val KEY_NOTIF_COLLAB_REQUESTS = "notifCollabRequests"
        const val KEY_NOTIF_MESSAGES = "notifMessages"
        const val KEY_NOTIF_COLLAB_UPDATES = "notifCollabUpdates"
        const val KEY_NOTIF_TASK_REMINDERS = "notifTaskReminders"
        const val KEY_NOTIF_DUE_DATE = "notifDueDate"
        const val KEY_NOTIF_COMPLETED_TASK = "notifCompletedTask"
        const val KEY_NOTIF_SECURITY = "notifSecurity"
        const val KEY_NOTIF_PRO_UPDATES = "notifProUpdates"
        const val KEY_NOTIF_MARKETING = "notifMarketing"
    }

    fun setNotificationSetting(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getNotificationSetting(key: String, defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveUserProfile(name: String, profession: String, bio: String, imageUrl: String?, followers: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PROFESSION, profession)
            putString(KEY_USER_BIO, bio)
            putString(KEY_USER_IMAGE, imageUrl)
            putLong(KEY_USER_FOLLOWERS, followers)
            apply()
        }
    }

    fun saveTasksCount(count: Int) {
        sharedPreferences.edit().putInt(KEY_TASKS_COUNT, count).apply()
    }

    fun saveCollabsCount(count: Int) {
        sharedPreferences.edit().putInt(KEY_COLLABS_COUNT, count).apply()
    }

    fun getUserName(): String = sharedPreferences.getString(KEY_USER_NAME, "Name") ?: "Name"
    fun getUserProfession(): String = sharedPreferences.getString(KEY_USER_PROFESSION, "") ?: ""
    fun getUserBio(): String = sharedPreferences.getString(KEY_USER_BIO, "") ?: ""
    fun getUserImage(): String? = sharedPreferences.getString(KEY_USER_IMAGE, null)
    fun getUserFollowers(): Long = sharedPreferences.getLong(KEY_USER_FOLLOWERS, 0L)
    fun getTasksCount(): Int = sharedPreferences.getInt(KEY_TASKS_COUNT, 0)
    fun getCollabsCount(): Int = sharedPreferences.getInt(KEY_COLLABS_COUNT, 0)

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
