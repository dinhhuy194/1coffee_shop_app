package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.Repository.FavoriteRepository
import com.example.coffeeshop.databinding.ActivityFavoriteBinding
import com.example.coffeeshop.Helper.BottomNavHelper
import com.google.firebase.auth.FirebaseAuth

class FavoriteActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFavoriteBinding
    private val favoriteRepository = FavoriteRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: PopularAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupRecyclerView()
        loadFavorites()
        BottomNavHelper.setup(this, BottomNavHelper.Tab.FAVOURITE)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh favorites when returning to this activity
        loadFavorites()
    }
    
    private fun setupUI() {
        binding.backBtn.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }
    
    private fun loadFavorites() {
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            // User not logged in
            binding.countTxt.text = "0 items"
            binding.emptyState.visibility = View.VISIBLE
            binding.favoriteRecyclerView.visibility = View.GONE
            Toast.makeText(this, "Vui lòng đăng nhập để xem yêu thích", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.favoriteRecyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        
        favoriteRepository.getFavorites(userId) { favoritesList ->
            // Update count
            binding.countTxt.text = "${favoritesList.size} items"
            
            // Show/hide empty state
            if (favoritesList.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.favoriteRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.favoriteRecyclerView.visibility = View.VISIBLE
                
                // Setup adapter with favorites
                adapter = PopularAdapter(favoritesList.toMutableList())
                binding.favoriteRecyclerView.adapter = adapter
            }
        }
    }
}
