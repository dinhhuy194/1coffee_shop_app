package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.example.coffeeshop.R
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.coffeeshop.Adapter.BannerAdapter
import com.example.coffeeshop.Adapter.CategoryAdapter
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.FilterOptions
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Fragment.FilterBottomSheet
import com.example.coffeeshop.Helper.BottomNavHelper
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivityMainBinding
import com.example.coffeeshop.ui.compose.AddToCartBubble
import com.example.coffeeshop.ui.compose.CartBubbleData
import com.example.coffeeshop.ui.compose.CartBubbleState
import com.example.project1762.Helper.ManagmentCart

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Compose state cho bong bóng
    private var bubbleData by mutableStateOf<CartBubbleData?>(null)

    // Cache toàn bộ items để filter in-memory
    private var allItemsList: List<ItemsModel> = emptyList()
    private var categoryList: List<CategoryModel> = emptyList()
    private var currentFilter: FilterOptions = FilterOptions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBubbleCompose()
        initBanner()
        initCategory()
        initPopular()
        initAllItems()
        initBottomMenu()
        initSearch()
        initFilter()
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerViewPopular.adapter?.notifyDataSetChanged()
        binding.recyclerViewAllItems.adapter?.notifyDataSetChanged()

        val pending = CartBubbleState.consume()
        if (pending != null) {
            bubbleData = pending
        }

        // Cập nhật cart badge mỗi khi quay lại màn hình
        updateCartBadge()

        // Xóa text search khi quay lại trang chủ
        binding.searchBox.text.clear()
    }

    // ──────────────── Cart Badge ────────────────
    /**
     * Cập nhật badge hiển thị số lượng sản phẩm trong giỏ hàng
     * trên bottom navigation bar.
     */
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

    // ──────────────── Bubble ────────────────
    private fun setupBubbleCompose() {
        binding.bubbleComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AddToCartBubble(
                    data = bubbleData,
                    onCartClicked = {
                        startActivity(Intent(this@MainActivity, CartActivity::class.java))
                    },
                    onDismiss = {
                        bubbleData = null
                    }
                )
            }
        }
    }

    // ──────────────── Bottom Nav ────────────────
    private fun initBottomMenu() {
        BottomNavHelper.setup(this, BottomNavHelper.Tab.EXPLORER)
    }

    // ──────────────── Banner Slider ────────────────
    private val bannerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val bannerRunnable = object : Runnable {
        override fun run() {
            val vp = binding.bannerViewPager
            val count = vp.adapter?.itemCount ?: 0
            if (count > 1) {
                vp.currentItem = (vp.currentItem + 1) % count
            }
            bannerHandler.postDelayed(this, 3000)
        }
    }

    private fun initBanner() {
        binding.progressBarBanner.visibility = View.VISIBLE
        viewModel.loadBanner().observe(this) { banners ->
            // Lọc banner ẩn
            val visibleBanners = banners.filter { !it.isHidden }

            if (visibleBanners.isEmpty()) {
                binding.progressBarBanner.visibility = View.GONE
                return@observe
            }

            // Set adapter
            binding.bannerViewPager.adapter = BannerAdapter(visibleBanners)
            binding.progressBarBanner.visibility = View.GONE

            // Setup dots indicator
            setupBannerDots(visibleBanners.size)

            // Theo dõi page change để cập nhật dots
            binding.bannerViewPager.registerOnPageChangeCallback(
                object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateBannerDots(position, visibleBanners.size)
                    }
                }
            )

            // Auto-scroll
            bannerHandler.removeCallbacks(bannerRunnable)
            if (visibleBanners.size > 1) {
                bannerHandler.postDelayed(bannerRunnable, 3000)
            }
        }
    }

    private fun setupBannerDots(count: Int) {
        val dotsLayout = binding.bannerDots
        dotsLayout.removeAllViews()
        if (count <= 1) return

        for (i in 0 until count) {
            val dot = View(this)
            val size = if (i == 0) 10 else 8
            val params = LinearLayout.LayoutParams(
                (size * resources.displayMetrics.density).toInt(),
                (size * resources.displayMetrics.density).toInt()
            )
            params.setMargins(
                (4 * resources.displayMetrics.density).toInt(), 0,
                (4 * resources.displayMetrics.density).toInt(), 0
            )
            dot.layoutParams = params
            dot.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(if (i == 0) getColor(R.color.darkBrown) else getColor(R.color.grey))
            }
            dotsLayout.addView(dot)
        }
    }

    private fun updateBannerDots(selected: Int, count: Int) {
        val dotsLayout = binding.bannerDots
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i)
            val isActive = i == selected
            val size = if (isActive) 10 else 8
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = (size * resources.displayMetrics.density).toInt()
            params.height = (size * resources.displayMetrics.density).toInt()
            dot.layoutParams = params
            (dot.background as? android.graphics.drawable.GradientDrawable)?.setColor(
                if (isActive) getColor(R.color.darkBrown) else getColor(R.color.grey)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    // ──────────────── Category ────────────────
    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.loadCategory().observe(this) { cats ->
            categoryList = cats
            binding.recyclerViewCat.layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.recyclerViewCat.adapter = CategoryAdapter(cats)
            binding.progressBarCategory.visibility = View.GONE
        }
    }

    // ──────────────── Popular items (từ node "Popular") ────────────────
    private fun initPopular() {
        binding.progressBarPopular.visibility = View.VISIBLE
        viewModel.loadPopular().observe(this) { items ->
            val popularItems = items.filter { it.picUrl.isNotEmpty() }
            binding.recyclerViewPopular.layoutManager = GridLayoutManager(this, 2)
            binding.recyclerViewPopular.adapter = PopularAdapter(popularItems.toMutableList())
            binding.progressBarPopular.visibility = View.GONE
        }
    }

    // ──────────────── All items (từ node "Items") với filter ────────────────
    private fun initAllItems() {
        binding.progressBarAllItems.visibility = View.VISIBLE
        viewModel.loadAllItems().observe(this) { items ->
            allItemsList = items.filter { it.picUrl.isNotEmpty() }
            binding.recyclerViewAllItems.layoutManager = GridLayoutManager(this, 2)
            applyAndRenderFilter()
            binding.progressBarAllItems.visibility = View.GONE
        }
    }

    // ──────────────── Filter ────────────────
    private fun initFilter() {
        binding.imgFilter.setOnClickListener {
            FilterBottomSheet.newInstance(
                current    = currentFilter,
                categories = categoryList
            ) { newOptions ->
                currentFilter = newOptions
                applyAndRenderFilter()
                updateFilterIndicator()
            }.show(supportFragmentManager, "filter")
        }
    }

    /**
     * Áp dụng filter hiện tại lên allItemsList và render vào RecyclerView.
     */
    private fun applyAndRenderFilter() {
        val filtered = viewModel.applyFilter(allItemsList, currentFilter)

        if (filtered.isEmpty()) {
            binding.recyclerViewAllItems.visibility = View.GONE
            binding.emptyFilterLayout.visibility = View.VISIBLE
        } else {
            binding.emptyFilterLayout.visibility = View.GONE
            binding.recyclerViewAllItems.visibility = View.VISIBLE
            if (binding.recyclerViewAllItems.layoutManager == null) {
                binding.recyclerViewAllItems.layoutManager = GridLayoutManager(this, 2)
            }
            binding.recyclerViewAllItems.adapter = PopularAdapter(filtered.toMutableList())
        }
    }

    /**
     * Highlight icon filter khi có bộ lọc đang active.
     */
    private fun updateFilterIndicator() {
        binding.imgFilter.alpha = if (currentFilter.isDefault) 1.0f else 0.6f
        // Nền icon đổi màu cam nếu filter đang active
        val bgResId = if (currentFilter.isDefault)
            com.example.coffeeshop.R.drawable.dark_brown_bg
        else
            com.example.coffeeshop.R.drawable.orange_bg
        binding.imgFilter.setBackgroundResource(bgResId)
    }

    // ──────────────── Search ────────────────
    private fun initSearch() {
        binding.searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    intent.putExtra("search_query", query)
                    startActivity(intent)
                    // Xóa text sau khi chuyển sang SearchActivity
                    binding.searchBox.text.clear()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }
}