package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.Review
import com.example.coffeeshop.Repository.ReviewRepository
import kotlinx.coroutines.launch

sealed class ReviewUiState {
    object Idle : ReviewUiState()
    object Loading : ReviewUiState()
    data class Success(val message: String) : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
    object AlreadyReviewed : ReviewUiState()
}

class ReviewViewModel : ViewModel() {

    private val repository = ReviewRepository()

    // ─── Submit review state ─────────────────────────────────────────────────
    private val _uiState = MutableLiveData<ReviewUiState>(ReviewUiState.Idle)
    val uiState: LiveData<ReviewUiState> = _uiState

    // ─── Reviews cho 1 sản phẩm (trang Detail) ──────────────────────────────
    private val _itemReviews = MutableLiveData<List<Review>>(emptyList())
    val itemReviews: LiveData<List<Review>> = _itemReviews

    private val _loadingItemReviews = MutableLiveData(false)
    val loadingItemReviews: LiveData<Boolean> = _loadingItemReviews

    // ─── Reviews của user (trang Profile) ────────────────────────────────────
    private val _userReviews = MutableLiveData<List<Review>>(emptyList())
    val userReviews: LiveData<List<Review>> = _userReviews

    private val _loadingUserReviews = MutableLiveData(false)
    val loadingUserReviews: LiveData<Boolean> = _loadingUserReviews

    // ─── Delete state ────────────────────────────────────────────────────────
    private val _deleteState = MutableLiveData<ReviewUiState>(ReviewUiState.Idle)
    val deleteState: LiveData<ReviewUiState> = _deleteState

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBMIT
    // ─────────────────────────────────────────────────────────────────────────

    fun submitReview(
        itemId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String
    ) {
        if (rating == 0) {
            _uiState.value = ReviewUiState.Error("Vui lòng chọn số sao đánh giá.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            try {
                val alreadyReviewed = repository.hasUserReviewed(itemId, userId)
                if (alreadyReviewed) {
                    _uiState.value = ReviewUiState.AlreadyReviewed
                    return@launch
                }

                repository.submitReviewAndUpdateRating(
                    itemId    = itemId,
                    userId    = userId,
                    userName  = userName,
                    rating    = rating,
                    comment   = comment
                )
                _uiState.value = ReviewUiState.Success("Cảm ơn bạn đã đánh giá! ⭐")

                // Tự động reload reviews sau khi submit
                loadReviewsForItem(itemId)
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(e.message ?: "Có lỗi xảy ra, thử lại sau.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD REVIEWS
    // ─────────────────────────────────────────────────────────────────────────

    /** Load reviews cho 1 sản phẩm (dùng trên trang Detail). */
    fun loadReviewsForItem(itemId: String) {
        viewModelScope.launch {
            _loadingItemReviews.value = true
            try {
                android.util.Log.d("ReviewVM", "Loading reviews for itemId='$itemId'")
                val reviews = repository.getReviewsForItem(itemId)
                android.util.Log.d("ReviewVM", "Loaded ${reviews.size} reviews for '$itemId'")
                _itemReviews.value = reviews
            } catch (e: Exception) {
                android.util.Log.e("ReviewVM", "Failed to load reviews: ${e.message}", e)
                _itemReviews.value = emptyList()
            } finally {
                _loadingItemReviews.value = false
            }
        }
    }

    /** Load reviews của user hiện tại (dùng trên trang MyReviews). */
    fun loadUserReviews(userId: String) {
        viewModelScope.launch {
            _loadingUserReviews.value = true
            try {
                val reviews = repository.getReviewsByUser(userId)
                _userReviews.value = reviews
            } catch (e: Exception) {
                _userReviews.value = emptyList()
            } finally {
                _loadingUserReviews.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIKE / UNLIKE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Toggle like cho 1 review.
     * Sau khi thành công, cập nhật lại danh sách hiện tại (in-memory) để
     * UI phản hồi ngay mà không cần re-fetch toàn bộ.
     */
    fun toggleLike(review: Review, userId: String) {
        viewModelScope.launch {
            try {
                val isNowLiked = repository.toggleLikeReview(review.reviewId, userId)

                // Cập nhật in-memory cho itemReviews
                _itemReviews.value = _itemReviews.value?.map { r ->
                    if (r.reviewId == review.reviewId) {
                        val newLikedBy = if (isNowLiked) {
                            r.likedBy + userId
                        } else {
                            r.likedBy - userId
                        }
                        r.copy(
                            likes = newLikedBy.size,
                            likedBy = newLikedBy
                        )
                    } else r
                }

                // Cập nhật in-memory cho userReviews (nếu đang hiện)
                _userReviews.value = _userReviews.value?.map { r ->
                    if (r.reviewId == review.reviewId) {
                        val newLikedBy = if (isNowLiked) {
                            r.likedBy + userId
                        } else {
                            r.likedBy - userId
                        }
                        r.copy(
                            likes = newLikedBy.size,
                            likedBy = newLikedBy
                        )
                    } else r
                }
            } catch (_: Exception) {
                // Bỏ qua lỗi like – không critical
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteReview(review: Review) {
        viewModelScope.launch {
            _deleteState.value = ReviewUiState.Loading
            try {
                repository.deleteReview(review)
                _deleteState.value = ReviewUiState.Success("Đã xóa đánh giá")

                // Xóa khỏi danh sách in-memory
                _itemReviews.value = _itemReviews.value?.filter { it.reviewId != review.reviewId }
                _userReviews.value = _userReviews.value?.filter { it.reviewId != review.reviewId }
            } catch (e: Exception) {
                _deleteState.value = ReviewUiState.Error(e.message ?: "Không thể xóa đánh giá")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESET
    // ─────────────────────────────────────────────────────────────────────────

    fun resetState() {
        _uiState.value = ReviewUiState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = ReviewUiState.Idle
    }
}
