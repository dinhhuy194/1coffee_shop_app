package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Repository.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders
    
    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState
    
    /**
     * Load order history for user
     */
    fun loadOrderHistory(userId: String) {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading
            
            val result = repository.getOrderHistory(userId)
            
            if (result.isSuccess) {
                _orders.value = result.getOrNull() ?: emptyList()
                _loadingState.value = LoadingState.Success
            } else {
                _loadingState.value = LoadingState.Error(result.exceptionOrNull()?.message ?: "Failed to load orders")
            }
        }
    }
    
    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }
}
