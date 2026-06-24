package com.durgesh.promoly.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.durgesh.promoly.R
import com.durgesh.promoly.fragments.AddTaskFragment
import com.durgesh.promoly.fragments.CollabsFragment
import com.durgesh.promoly.fragments.HomeFragment
import com.durgesh.promoly.fragments.ProfileFragment
import com.durgesh.promoly.fragments.TasksFragment
import com.durgesh.promoly.util.FcmUtils
import com.durgesh.promoly.util.replaceFragment
import com.durgesh.promoly.util.showToast
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabCenter: FloatingActionButton

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(this, "Notification permission denied. You won't receive updates.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomAppBar = findViewById(R.id.bottomAppBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabCenter = findViewById(R.id.fabCenter)

        // Request Notification Permission for Android 13+
        askNotificationPermission()

        // Update FCM Token on start
        FcmUtils.updateFcmToken()

        // Ensure bottom bar doesn't add system padding
        bottomAppBar.setOnApplyWindowInsetsListener { _, insets -> insets }
        bottomNavigationView.background = null

        if (savedInstanceState == null) {
            supportFragmentManager.replaceFragment(R.id.fragment_container, HomeFragment())
            bottomNavigationView.selectedItemId = R.id.navigation_home
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.replaceFragment(R.id.fragment_container, HomeFragment())
                    true
                }
                R.id.navigation_collabs -> {
                    supportFragmentManager.replaceFragment(R.id.fragment_container, CollabsFragment())
                    true
                }
                R.id.navigation_tasks -> {
                    supportFragmentManager.replaceFragment(R.id.fragment_container, TasksFragment())
                    true
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.replaceFragment(R.id.fragment_container, ProfileFragment())
                    true
                }
                else -> false
            }
        }

        fabCenter.setOnClickListener {
            supportFragmentManager.replaceFragment(R.id.fragment_container, AddTaskFragment())
            true
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}