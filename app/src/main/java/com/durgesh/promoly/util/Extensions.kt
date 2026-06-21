package com.durgesh.promoly.util

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.durgesh.promoly.R

/**
 * Common extension for showing Toast messages
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

/**
 * Common extension for replacing fragments
 */
fun FragmentManager.replaceFragment(containerId: Int, fragment: Fragment, addToBackStack: Boolean = false) {
    val transaction = beginTransaction()
        .replace(containerId, fragment)
    if (addToBackStack) {
        transaction.addToBackStack(null)
    }
    transaction.commit()
}
