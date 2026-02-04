package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MainViewModel by viewModels()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchQuery = intent.getStringExtra("search_query") ?: ""

        binding.backBtn.setOnClickListener { finish() }
        binding.searchQueryTxt.text = "\"$searchQuery\""

        performSearch()
    }

    private fun performSearch() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsTxt.visibility = View.GONE
        binding.popularSection.visibility = View.GONE
        
        viewModel.searchItems(searchQuery).observe(this) { searchResults ->
            if (searchResults.isNotEmpty()) {
                // Show search results
                binding.searchResultsRecyclerView.apply {
                    layoutManager = GridLayoutManager(this@SearchActivity, 2)
                    adapter = PopularAdapter(searchResults)
                }
                binding.noResultsTxt.visibility = View.GONE
                binding.popularSection.visibility = View.GONE
            } else {
                // No results found - show popular items instead
                binding.noResultsTxt.visibility = View.VISIBLE
                binding.popularSection.visibility = View.VISIBLE
                
                // Load popular items as suggestions
                viewModel.loadPopular().observe(this) { popularItems ->
                    binding.searchResultsRecyclerView.apply {
                        layoutManager = GridLayoutManager(this@SearchActivity, 2)
                        adapter = PopularAdapter(popularItems)
                    }
                }
            }
            binding.progressBar.visibility = View.GONE
        }
    }
}
