package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.AuthViewModel
import com.example.coffeeshop.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                viewModel.loginWithGoogle(token)
            } ?: run {
                Toast.makeText(this, "Google Sign-In failed: No ID token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupGoogleSignIn()
        setupListeners()
        observeAuthState()
    }
    
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    private fun setupListeners() {
        binding.apply {
            loginBtn.setOnClickListener {
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                
                when {
                    email.isEmpty() -> {
                        Toast.makeText(this@LoginActivity, "Please enter email", Toast.LENGTH_SHORT).show()
                    }
                    password.isEmpty() -> {
                        Toast.makeText(this@LoginActivity, "Please enter password", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        viewModel.login(email, password)
                    }
                }
            }
            
            googleSignInBtn.setOnClickListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
            
            registerLink.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
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
                    Toast.makeText(this, "Welcome ${state.user.name}!", Toast.LENGTH_SHORT).show()
                    
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
