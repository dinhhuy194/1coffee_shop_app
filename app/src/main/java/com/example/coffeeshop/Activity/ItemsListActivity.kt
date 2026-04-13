package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.ItemsListCategoryAdapter
import com.example.coffeeshop.Helper.BottomNavHelper
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivityItemsListBinding
import com.example.coffeeshop.ui.compose.AddToCartBubble
import com.example.coffeeshop.ui.compose.CartBubbleData
import com.example.coffeeshop.ui.compose.CartBubbleState
import com.example.project1762.Helper.ManagmentCart

class ItemsListActivity : AppCompatActivity() {
    lateinit var binding: ActivityItemsListBinding
    private val viewModel: MainViewModel by viewModels()
    private var id: String = ""
    private var title: String = ""

    // Compose state cho bong bóng
    private var bubbleData by mutableStateOf<CartBubbleData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityItemsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBubbleCompose()
        getBundle()
        initList()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh items to sync favorite status
        binding.listView.adapter?.notifyDataSetChanged()

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
                        startActivity(Intent(this@ItemsListActivity, CartActivity::class.java))
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

    private fun initList() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            viewModel.loadItemCategory(id).observe(this@ItemsListActivity) { items ->
                listView.layoutManager = LinearLayoutManager(
                    this@ItemsListActivity,
                    LinearLayoutManager.VERTICAL, false
                )
                listView.adapter = ItemsListCategoryAdapter(items)
                progressBar.visibility = View.GONE
            }
            backBtn.setOnClickListener {
                finish()
            }
        }
    }

    private fun getBundle() {
        id = intent.getStringExtra("id")!!
        title = intent.getStringExtra("title")!!
        binding.categoryTxt.text = title
    }
}
