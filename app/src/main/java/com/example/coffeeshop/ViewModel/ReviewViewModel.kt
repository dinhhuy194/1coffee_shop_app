package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _uiState = MutableLiveData<ReviewUiState>(ReviewUiState.Idle)
    val uiState: LiveData<ReviewUiState> = _uiState

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
                // Kiểm tra xem user đã review món này chưa
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
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(e.message ?: "Có lỗi xảy ra, thử lại sau.")
            }
        }
    }

    fun resetState() {
        _uiState.value = ReviewUiState.Idle
    }
}
