package com.durgesh.promoly.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.durgesh.promoly.R
import com.durgesh.promoly.adapter.AdapterUserSearch
import com.durgesh.promoly.model.ModelUserSearch
import com.durgesh.promoly.util.Constants
import com.durgesh.promoly.util.showToast
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SearchUserActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdapterUserSearch
    private val userList = mutableListOf<ModelUserSearch>()
    
    private lateinit var rvResults: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var llNoResults: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_user)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.llHeaderSearch)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        rvResults = findViewById(R.id.rvUserSearchResults)
        etSearch = findViewById(R.id.etSearchUser)
        btnBack = findViewById(R.id.btnBackSearch)
        btnSearch = findViewById(R.id.btnDoSearch)
        llNoResults = findViewById(R.id.llNoResults)
        setupRecyclerView()
        setupListeners()
    }


    private fun setupRecyclerView() {
        rvResults.layoutManager = LinearLayoutManager(this)
        adapter = AdapterUserSearch(userList) { user ->
            val intent = Intent(this, ViewProfile::class.java)
            intent.putExtra("userId", user.userId)
            startActivity(intent)
        }
        rvResults.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else false
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    performSearch(query)
                } else if (query.isEmpty()) {
                    userList.clear()
                    adapter.notifyDataSetChanged()
                    llNoResults.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return

        // Multi-case search to be safe
        val capitalizedQuery = q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        db.collection(Constants.COLLECTION_USERS)
            .whereGreaterThanOrEqualTo("name", capitalizedQuery)
            .whereLessThanOrEqualTo("name", capitalizedQuery + "\uf8ff")
            .limit(15) // Show more in full activity
            .get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (doc in documents) {
                    val user = ModelUserSearch(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    )
                    userList.add(user)
                }
                
                // Fallback for non-capitalized if first search was capitalized
                if (userList.isEmpty() && q != capitalizedQuery) {
                    db.collection(Constants.COLLECTION_USERS)
                        .whereGreaterThanOrEqualTo("name", q)
                        .whereLessThanOrEqualTo("name", q + "\uf8ff")
                        .limit(15)
                        .get()
                        .addOnSuccessListener { docs ->
                            for (doc in docs) {
                                val user = ModelUserSearch(
                                    userId = doc.id,
                                    name = doc.getString("name") ?: "",
                                    profileImageUrl = doc.getString("profileImageUrl") ?: ""
                                )
                                userList.add(user)
                            }
                            updateUI()
                        }
                } else {
                    updateUI()
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchUserActivity", "Error searching", e)
                showToast("Search failed: ${e.message}")
            }
    }

    private fun updateUI() {
        adapter.notifyDataSetChanged()
        if (userList.isEmpty()) {
            llNoResults.visibility = View.VISIBLE
            rvResults.visibility = View.GONE
        } else {
            llNoResults.visibility = View.GONE
            rvResults.visibility = View.VISIBLE
        }
    }
}