package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.Repository.AuthRepository
import com.example.coffeeshop.Repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()
    
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile
    
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    private val _logoutState = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutState: LiveData<LogoutState> = _logoutState
    
    /**
     * Load user profile
     */
    fun loadProfile() {
        viewModelScope.launch {
            val isUserLoggedIn = authRepository.isLoggedIn()
            _isLoggedIn.value = isUserLoggedIn
            
            if (isUserLoggedIn) {
                val user = authRepository.getCurrentUser()
                _userProfile.value = user
            } else {
                _userProfile.value = null
            }
        }
    }
    
    /**
     * Logout
     */
    fun logout(googleSignInClient: GoogleSignInClient? = null) {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading
            
            try {
                // Sign out from Firebase
                authRepository.signOut()
                
                // Sign out from Google if client provided
                googleSignInClient?.signOut()?.await()
                
                _userProfile.value = null
                _isLoggedIn.value = false
                _logoutState.value = LogoutState.Success
            } catch (e: Exception) {
                _logoutState.value = LogoutState.Error(e.message ?: "Logout failed")
            }
        }
    }
    
    sealed class LogoutState {
        object Idle : LogoutState()
        object Loading : LogoutState()
        object Success : LogoutState()
        data class Error(val message: String) : LogoutState()
    }
}
