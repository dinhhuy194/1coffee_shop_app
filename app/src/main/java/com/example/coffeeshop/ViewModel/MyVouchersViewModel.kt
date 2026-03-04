package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.RedeemedVoucher
import com.example.coffeeshop.Repository.UserRepository
import kotlinx.coroutines.launch

class MyVouchersViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _vouchers = MutableLiveData<List<RedeemedVoucher>>(emptyList())
    val vouchers: LiveData<List<RedeemedVoucher>> = _vouchers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    /**
     * Load danh sách voucher đã đổi
     */
    fun loadVouchers(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            userRepository.getRedeemedVouchers(userId)
                .onSuccess { list ->
                    _vouchers.value = list
                }
                .onFailure { e ->
                    _actionState.value = ActionState.Error(e.message ?: "Lỗi tải voucher")
                }

            _isLoading.value = false
        }
    }

    /**
     * Đánh dấu voucher đã sử dụng
     */
    fun markAsUsed(userId: String, voucherId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading

            userRepository.markVoucherUsed(userId, voucherId)
                .onSuccess {
                    _actionState.value = ActionState.Success("Đã sử dụng voucher! ✅")
                    // Refresh danh sách
                    loadVouchers(userId)
                }
                .onFailure { e ->
                    _actionState.value = ActionState.Error(e.message ?: "Lỗi cập nhật voucher")
                }
        }
    }

    /**
     * Reset state
     */
    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        data class Success(val message: String) : ActionState()
        data class Error(val message: String) : ActionState()
    }
}
