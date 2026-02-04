package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Repository.OrderRepository
import kotlinx.coroutines.launch

class CheckoutViewModel : ViewModel() {
    private val repository = OrderRepository()
    
    private val _checkoutState = MutableLiveData<CheckoutState>(CheckoutState.Idle)
    val checkoutState: LiveData<CheckoutState> = _checkoutState
    
    /**
     * Place order
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
            
            _checkoutState.value = if (result.isSuccess) {
                CheckoutState.Success(result.getOrNull() ?: "")
            } else {
                CheckoutState.Error(result.exceptionOrNull()?.message ?: "Order failed")
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
