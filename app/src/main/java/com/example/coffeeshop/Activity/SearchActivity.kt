package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.FilterOptions
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Fragment.FilterBottomSheet
import com.example.coffeeshop.Helper.BottomNavHelper
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MainViewModel by viewModels()
    private var searchQuery: String = ""

    // ── Filter state ──
    private var currentFilter: FilterOptions = FilterOptions()
    private var categoryList: List<CategoryModel> = emptyList()
    private var searchResultsList: List<ItemsModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchQuery = intent.getStringExtra("search_query") ?: ""

        // Hiển thị query trong search box
        binding.searchBox.setText(searchQuery)
        binding.searchQueryTxt.text = "Kết quả: \"$searchQuery\""

        // Back button — chỉ cần finish() để quay lại MainActivity
        binding.backBtn.setOnClickListener { finish() }

        // Setup search box cho phép tìm kiếm lại
        setupSearchBox()

        // Setup bottom navigation
        initBottomMenu()

        // Load danh mục cho bộ lọc
        loadCategories()

        // Setup bộ lọc
        initFilter()

        // Thực hiện tìm kiếm
        performSearch()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại adapter khi quay lại (ví dụ sau khi xem chi tiết sản phẩm)
        binding.searchResultsRecyclerView.adapter?.notifyDataSetChanged()
    }

    /**
     * Setup thanh search cho phép người dùng tìm kiếm mới
     * ngay trên trang kết quả, không cần quay lại trang chủ.
     */
    private fun setupSearchBox() {
        binding.searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchBox.text.toString().trim()
                if (query.isNotEmpty() && query != searchQuery) {
                    searchQuery = query
                    binding.searchQueryTxt.text = "Kết quả: \"$searchQuery\""
                    // Reset filter khi search mới
                    currentFilter = FilterOptions()
                    updateFilterIndicator()
                    performSearch()
                }
                // Ẩn bàn phím
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    /**
     * Setup bottom navigation bar — highlight tab Explorer
     * vì Search là sub-screen của Explorer.
     */
    private fun initBottomMenu() {
        BottomNavHelper.setup(this, BottomNavHelper.Tab.EXPLORER)
    }

    /**
     * Load danh mục sản phẩm từ Firebase để dùng trong bộ lọc.
     */
    private fun loadCategories() {
        viewModel.loadCategory().observe(this) { cats ->
            categoryList = cats
        }
    }

    /**
     * Setup nút bộ lọc — mở FilterBottomSheet khi nhấn.
     */
    private fun initFilter() {
        binding.imgFilter.setOnClickListener {
            FilterBottomSheet.newInstance(
                current    = currentFilter,
                categories = categoryList
            ) { newOptions ->
                currentFilter = newOptions
                updateFilterIndicator()
                applyAndRenderFilter()
            }.show(supportFragmentManager, "filter")
        }
    }

    /**
     * Highlight icon filter khi có bộ lọc đang active.
     */
    private fun updateFilterIndicator() {
        binding.imgFilter.alpha = if (currentFilter.isDefault) 1.0f else 0.6f
        val bgResId = if (currentFilter.isDefault)
            com.example.coffeeshop.R.drawable.dark_brown_bg
        else
            com.example.coffeeshop.R.drawable.orange_bg
        binding.imgFilter.setBackgroundResource(bgResId)
    }

    /**
     * Áp dụng filter lên kết quả search hiện tại và render lại RecyclerView.
     */
    private fun applyAndRenderFilter() {
        val filtered = viewModel.applyFilter(searchResultsList, currentFilter)

        if (filtered.isEmpty() && searchResultsList.isNotEmpty()) {
            // Có kết quả search nhưng filter lọc hết
            binding.noResultsTxt.visibility = View.VISIBLE
            binding.noResultsTxt.text = "Không có sản phẩm phù hợp với bộ lọc"
            binding.popularSection.visibility = View.GONE
            binding.searchResultsRecyclerView.apply {
                layoutManager = GridLayoutManager(this@SearchActivity, 2)
                adapter = PopularAdapter(mutableListOf())
            }
        } else {
            binding.noResultsTxt.visibility = View.GONE
            binding.popularSection.visibility = View.GONE
            binding.searchResultsRecyclerView.apply {
                layoutManager = GridLayoutManager(this@SearchActivity, 2)
                adapter = PopularAdapter(filtered.toMutableList())
            }
        }

        // Cập nhật tiêu đề hiển thị số kết quả
        val countText = if (currentFilter.isDefault) {
            "Kết quả: \"$searchQuery\""
        } else {
            "Kết quả: \"$searchQuery\" (${filtered.size})"
        }
        binding.searchQueryTxt.text = countText
    }

    private fun performSearch() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsTxt.visibility = View.GONE
        binding.popularSection.visibility = View.GONE

        viewModel.searchItems(searchQuery).observe(this) { searchResults ->
            searchResultsList = searchResults

            if (searchResults.isNotEmpty()) {
                // Áp dụng filter lên kết quả (mặc định filter = không lọc gì)
                applyAndRenderFilter()
            } else {
                // No results found - show popular items instead
                binding.noResultsTxt.visibility = View.VISIBLE
                binding.noResultsTxt.text = "Không tìm thấy kết quả"
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
    }
}
