package com.example.coffeeshop.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.coffeeshop.Activity.LoginActivity
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.ProfileViewModel
import com.example.coffeeshop.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupGoogleSignIn()
        setupListeners()
        observeViewModel()
        
        viewModel.loadProfile()
    }
    
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }
    
    private fun setupListeners() {
        binding.apply {
            // Back button
            backBtn.setOnClickListener {
                requireActivity().finish()
            }
            
            // Auth button (Login/Logout)
            authBtn.setOnClickListener {
                viewModel.isLoggedIn.value?.let { isLoggedIn ->
                    if (isLoggedIn) {
                        // Logout
                        viewModel.logout(googleSignInClient)
                    } else {
                        // Go to Login
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                    }
                }
            }
            
            // Menu items - show toast for now (UI only as requested)
            favoritesBtn.setOnClickListener {
                startActivity(Intent(requireContext(), com.example.coffeeshop.Activity.FavoriteActivity::class.java))
            }
            
            paymentBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Payment Methods - Coming soon", Toast.LENGTH_SHORT).show()
            }
            
            settingsBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Settings - Coming soon", Toast.LENGTH_SHORT).show()
            }
            
            languageBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Language - Coming soon", Toast.LENGTH_SHORT).show()
            }
            
            aboutBtn.setOnClickListener {
                Toast.makeText(requireContext(), "About Us - Coming soon", Toast.LENGTH_SHORT).show()
            }
            
            feedbackBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Feedback - Coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe login state
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            binding.authBtn.text = if (isLoggedIn) "Logout" else "Login"
        }
        
        // Observe user profile
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.apply {
                if (user != null) {
                    nameTxt.text = user.name
                    emailTxt.text = user.email
                    
                    // Load avatar if available
                    user.photoUrl?.let { url ->
                        Glide.with(this@ProfileFragment)
                            .load(url)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(avatarImg)
                    }
                } else {
                    nameTxt.text = "Guest User"
                    emailTxt.text = "Login to access more features"
                    avatarImg.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
        }
        
        // Observe logout state
        viewModel.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.LogoutState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ProfileViewModel.LogoutState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
                is ProfileViewModel.LogoutState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Logout error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
