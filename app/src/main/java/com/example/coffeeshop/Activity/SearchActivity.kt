package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.FilterOptions
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Fragment.FilterBottomSheet
import com.example.coffeeshop.Helper.BottomNavHelper
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivitySearchBinding
import com.example.coffeeshop.ui.compose.AddToCartBubble
import com.example.coffeeshop.ui.compose.CartBubbleData
import com.example.coffeeshop.ui.compose.CartBubbleState
import com.example.project1762.Helper.ManagmentCart

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MainViewModel by viewModels()
    private var searchQuery: String = ""

    // ── Filter state ──
    private var currentFilter: FilterOptions = FilterOptions()
    private var categoryList: List<CategoryModel> = emptyList()
    private var searchResultsList: List<ItemsModel> = emptyList()

    // Tracking LiveData để hủy observer cũ khi search lại
    private var currentSearchLiveData: LiveData<MutableList<ItemsModel>>? = null
    private var currentSearchObserver: Observer<MutableList<ItemsModel>>? = null

    // Chế độ: true = chỉ dùng filter (không có search query)
    private var isFilterOnlyMode: Boolean = false

    // Compose state cho bong bóng
    private var bubbleData by mutableStateOf<CartBubbleData?>(null)

    companion object {
        const val EXTRA_SEARCH_QUERY = "search_query"
        const val EXTRA_FILTER = "filter_options"
        const val EXTRA_FILTER_ONLY = "filter_only_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBubbleCompose()

        // Đọc intent extras
        searchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY) ?: ""
        isFilterOnlyMode = intent.getBooleanExtra(EXTRA_FILTER_ONLY, false)

        // Nhận filter từ intent (nếu có)
        val intentFilter = intent.getSerializableExtra(EXTRA_FILTER) as? FilterOptions
        if (intentFilter != null) {
            currentFilter = intentFilter
        }

        // Hiển thị query trong search box
        binding.searchBox.setText(searchQuery)
        updateHeaderTitle()

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
        updateFilterIndicator()

        // Thực hiện tìm kiếm hoặc load tất cả
        if (isFilterOnlyMode && searchQuery.isEmpty()) {
            loadAllAndApplyFilter()
        } else {
            performSearch()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại adapter khi quay lại (ví dụ sau khi xem chi tiết sản phẩm)
        binding.searchResultsRecyclerView.adapter?.notifyDataSetChanged()

        // Show cart bubble if pending
        val pending = CartBubbleState.consume()
        if (pending != null) {
            bubbleData = pending
        }

        // Update cart badge
        updateCartBadge()
    }

    // ──────────────── Bubble ────────────────
    private fun setupBubbleCompose() {
        binding.bubbleComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AddToCartBubble(
                    data = bubbleData,
                    onCartClicked = {
                        startActivity(Intent(this@SearchActivity, CartActivity::class.java))
                    },
                    onDismiss = {
                        bubbleData = null
                    }
                )
            }
        }
    }

    // ──────────────── Cart Badge ────────────────
    private fun updateCartBadge() {
        val cartBadge = findViewById<TextView>(R.id.cartBadge) ?: return
        val managmentCart = ManagmentCart(this)
        val itemCount = managmentCart.getListCart().size

        if (itemCount > 0) {
            cartBadge.visibility = View.VISIBLE
            cartBadge.text = if (itemCount > 99) "99+" else itemCount.toString()
        } else {
            cartBadge.visibility = View.GONE
        }
    }

    /**
     * Setup thanh search cho phép người dùng tìm kiếm mới
     * ngay trên trang kết quả, không cần quay lại trang chủ.
     */
    private fun setupSearchBox() {
        binding.searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchQuery = query
                    isFilterOnlyMode = false
                    // Reset filter khi search mới
                    currentFilter = FilterOptions()
                    updateFilterIndicator()
                    updateHeaderTitle()
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

                if (searchQuery.isEmpty()) {
                    // Chế độ filter-only: load tất cả items rồi filter
                    isFilterOnlyMode = true
                    loadAllAndApplyFilter()
                } else {
                    // Đang có query search: filter trên kết quả search hiện tại
                    applyAndRenderFilter()
                }
            }.show(supportFragmentManager, "filter")
        }
    }

    /**
     * Highlight icon filter khi có bộ lọc đang active.
     */
    private fun updateFilterIndicator() {
        binding.imgFilter.alpha = if (currentFilter.isDefault) 1.0f else 0.6f
        val bgResId = if (currentFilter.isDefault)
            R.drawable.dark_brown_bg
        else
            R.drawable.orange_bg
        binding.imgFilter.setBackgroundResource(bgResId)
    }

    /**
     * Cập nhật tiêu đề hiển thị.
     */
    private fun updateHeaderTitle() {
        if (isFilterOnlyMode && searchQuery.isEmpty()) {
            val filterCount = getActiveFilterCount()
            binding.searchQueryTxt.text = if (filterCount > 0) {
                "Kết quả lọc ($filterCount bộ lọc)"
            } else {
                "Tất cả sản phẩm"
            }
        } else {
            binding.searchQueryTxt.text = "Kết quả: \"$searchQuery\""
        }
    }

    /**
     * Đếm số bộ lọc đang active.
     */
    private fun getActiveFilterCount(): Int {
        var count = 0
        if (currentFilter.categoryId.isNotEmpty()) count++
        if (currentFilter.minPrice > 0 || currentFilter.maxPrice < Double.MAX_VALUE) count++
        if (currentFilter.sortBy != FilterOptions.SortBy.DEFAULT) count++
        return count
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
        } else if (filtered.isEmpty()) {
            binding.noResultsTxt.visibility = View.VISIBLE
            binding.noResultsTxt.text = "Không tìm thấy kết quả"
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

        // Cập nhật tiêu đề
        if (isFilterOnlyMode && searchQuery.isEmpty()) {
            val filterCount = getActiveFilterCount()
            binding.searchQueryTxt.text = if (filterCount > 0) {
                "Kết quả lọc: ${filtered.size} sản phẩm"
            } else {
                "Tất cả sản phẩm (${filtered.size})"
            }
        } else {
            val countText = if (currentFilter.isDefault) {
                "Kết quả: \"$searchQuery\""
            } else {
                "Kết quả: \"$searchQuery\" (${filtered.size})"
            }
            binding.searchQueryTxt.text = countText
        }
    }

    /**
     * Load tất cả items (Items + Popular) rồi áp dụng filter.
     * Dùng cho chế độ filter-only (không có search query).
     */
    private fun loadAllAndApplyFilter() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsTxt.visibility = View.GONE
        binding.popularSection.visibility = View.GONE

        // Hủy observer cũ
        currentSearchLiveData?.let { ld ->
            currentSearchObserver?.let { obs -> ld.removeObserver(obs) }
        }

        val liveData = viewModel.loadAllItemsAndPopular()
        val observer = Observer<MutableList<ItemsModel>> { allItems ->
            searchResultsList = allItems
            applyAndRenderFilter()
            binding.progressBar.visibility = View.GONE
        }

        currentSearchLiveData = liveData
        currentSearchObserver = observer
        liveData.observe(this, observer)
    }

    /**
     * Thực hiện tìm kiếm theo query (search cả Items + Popular).
     */
    private fun performSearch() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsTxt.visibility = View.GONE
        binding.popularSection.visibility = View.GONE

        // Hủy observer cũ trước khi tạo mới (tránh nhận kết quả cũ)
        currentSearchLiveData?.let { liveData ->
            currentSearchObserver?.let { observer ->
                liveData.removeObserver(observer)
            }
        }

        val liveData = viewModel.searchItems(searchQuery)
        val observer = Observer<MutableList<ItemsModel>> { searchResults ->
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

        currentSearchLiveData = liveData
        currentSearchObserver = observer
        liveData.observe(this, observer)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
    }
}
