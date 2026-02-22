package com.example.coffeeshop.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Repository.OrderRepository
import com.example.coffeeshop.Repository.UserRepository
import kotlinx.coroutines.launch

class CheckoutViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val userRepository = UserRepository()
    
    private val _checkoutState = MutableLiveData<CheckoutState>(CheckoutState.Idle)
    val checkoutState: LiveData<CheckoutState> = _checkoutState
    
    /**
     * Place order và tự động cộng điểm BEAN
     */
    fun placeOrder(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            
            val result = repository.createOrder(userId, items, subtotal, tax, delivery, totalAmount)
            
            if (result.isSuccess) {
                val orderId = result.getOrNull() ?: ""
                
                // Cộng điểm BEAN sau khi đặt hàng thành công
                val orderAmountLong = totalAmount.toLong()
                userRepository.processPointsEarned(userId, orderAmountLong, orderId)
                    .onSuccess { beansEarned ->
                        Log.d("CheckoutVM", "Cộng $beansEarned BEAN cho đơn hàng #$orderId")
                    }
                    .onFailure { e ->
                        Log.e("CheckoutVM", "Lỗi cộng BEAN: ${e.message}")
                    }
                
                _checkoutState.value = CheckoutState.Success(orderId)
            } else {
                _checkoutState.value = CheckoutState.Error(
                    result.exceptionOrNull()?.message ?: "Order failed"
                )
            }
        }
    }
    
    sealed class CheckoutState {
        object Idle : CheckoutState()
        object Loading : CheckoutState()
        data class Success(val orderId: String) : CheckoutState()
        data class Error(val message: String) : CheckoutState()
    }
}
