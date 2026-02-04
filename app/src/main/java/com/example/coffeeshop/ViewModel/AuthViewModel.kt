package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.Repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    /**
     * Register new user with email and password
     */
    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.register(email, password, name)
            
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.login(email, password)
            
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }
    
    /**
     * Login with Google
     */
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.signInWithGoogle(idToken)
            
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Google sign-in failed")
            }
        }
    }
    
    /**
     * Reset auth state
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
    
    /**
     * Auth states
     */
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
