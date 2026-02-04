package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.coffeeshop.Adapter.CategoryAdapter
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBanner()
        initCategory()
        initPopular()
        initBottomMenu()
        initSearch()
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, CartActivity::class.java))
        }
        
        binding.myOrderBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, OrderHistoryActivity::class.java))
        }
        
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
        }
    }

    private fun initBanner() {
        binding.progressBarBanner.visibility = View.VISIBLE
        viewModel.loadBanner().observe(this) {
            Glide.with(this@MainActivity)
                .load(it[0].url)
                .into(binding.banner)
            binding.progressBarBanner.visibility = View.GONE
        }
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.loadCategory().observe(this) {
            binding.recyclerViewCat.layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.recyclerViewCat.adapter = CategoryAdapter(it)
            binding.progressBarCategory.visibility = View.GONE
        }
    }

    private fun initPopular() {
        binding.progressBarPopular.visibility = View.VISIBLE
        viewModel.loadPopular().observe(this) { items ->
            binding.recyclerViewPopular.layoutManager = GridLayoutManager(this, 2)
            binding.recyclerViewPopular.adapter = PopularAdapter(items)
            binding.progressBarPopular.visibility = View.GONE
        }
    }

    private fun initSearch() {
        binding.searchBox.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchBox.text.toString()
                if (query.isNotEmpty()) {
                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    intent.putExtra("search_query", query)
                    startActivity(intent)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }
}