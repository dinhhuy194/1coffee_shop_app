package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.ViewModel.AuthViewModel
import com.example.coffeeshop.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        observeAuthState()
    }
    
    private fun setupListeners() {
        binding.apply {
            registerBtn.setOnClickListener {
                val name = nameEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()
                
                when {
                    name.isEmpty() -> {
                        Toast.makeText(this@RegisterActivity, "Please enter name", Toast.LENGTH_SHORT).show()
                    }
                    email.isEmpty() -> {
                        Toast.makeText(this@RegisterActivity, "Please enter email", Toast.LENGTH_SHORT).show()
                    }
                    password.isEmpty() -> {
                        Toast.makeText(this@RegisterActivity, "Please enter password", Toast.LENGTH_SHORT).show()
                    }
                    password.length < 6 -> {
                        Toast.makeText(this@RegisterActivity, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                    password != confirmPassword -> {
                        Toast.makeText(this@RegisterActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        viewModel.register(email, password, name)
                    }
                }
            }
            
            loginLink.setOnClickListener {
                finish()
            }
        }
    }
    
    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is AuthViewModel.AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration successful! Welcome ${state.user.name}", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
