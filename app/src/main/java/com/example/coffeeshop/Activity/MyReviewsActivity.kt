package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.ReviewAdapter
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.ReviewUiState
import com.example.coffeeshop.ViewModel.ReviewViewModel
import com.google.firebase.auth.FirebaseAuth

class MyReviewsActivity : AppCompatActivity() {

    private val reviewViewModel: ReviewViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var reviewAdapter: ReviewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_reviews)

        setupToolbar()
        setupRecyclerView()
        observeData()
        loadData()
    }

    private fun setupToolbar() {
        findViewById<View>(R.id.backBtn).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val userId = auth.currentUser?.uid

        reviewAdapter = ReviewAdapter(
            reviews       = emptyList(),
            currentUserId = userId,
            showItemName  = true, // Hiển thị tên sản phẩm trong danh sách của tôi
            onLikeClick   = { review ->
                if (userId != null) {
                    reviewViewModel.toggleLike(review, userId)
                }
            },
            onDeleteClick = { review ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa đánh giá")
                    .setMessage("Bạn có chắc chắn muốn xóa đánh giá cho \"${review.itemId}\"?")
                    .setPositiveButton("Xóa") { _, _ ->
                        reviewViewModel.deleteReview(review)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewReviews)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = reviewAdapter
    }

    private fun observeData() {
        // Loading state
        reviewViewModel.loadingUserReviews.observe(this) { isLoading ->
            findViewById<View>(R.id.progressBar).visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        // Reviews list
        reviewViewModel.userReviews.observe(this) { reviews ->
            reviewAdapter?.updateList(reviews)
            findViewById<android.widget.TextView>(R.id.reviewCountTxt).text =
                "${reviews.size} đánh giá"

            if (reviews.isEmpty() && reviewViewModel.loadingUserReviews.value != true) {
                findViewById<View>(R.id.emptyLayout).visibility = View.VISIBLE
                findViewById<View>(R.id.recyclerViewReviews).visibility = View.GONE
            } else {
                findViewById<View>(R.id.emptyLayout).visibility = View.GONE
                findViewById<View>(R.id.recyclerViewReviews).visibility = View.VISIBLE
            }
        }

        // Delete state
        reviewViewModel.deleteState.observe(this) { state ->
            when (state) {
                is ReviewUiState.Success -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    reviewViewModel.resetDeleteState()
                }
                is ReviewUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    reviewViewModel.resetDeleteState()
                }
                else -> Unit
            }
        }
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            reviewViewModel.loadUserReviews(userId)
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
