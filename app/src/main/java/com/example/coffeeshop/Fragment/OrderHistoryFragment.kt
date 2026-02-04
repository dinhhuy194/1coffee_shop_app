package com.example.coffeeshop.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Activity.LoginActivity
import com.example.coffeeshop.Adapter.OrderAdapter
import com.example.coffeeshop.ViewModel.OrderViewModel
import com.example.coffeeshop.databinding.FragmentOrderHistoryBinding
import com.google.firebase.auth.FirebaseAuth

class OrderHistoryFragment : Fragment() {
    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkAuthAndLoadOrders()
    }
    
    private fun setupRecyclerView() {
        binding.ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            requireActivity().finish()
        }
        
        binding.loginBtn.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        // Observe orders
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            if (orders.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.ordersRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.ordersRecyclerView.visibility = View.VISIBLE
                binding.ordersRecyclerView.adapter = OrderAdapter(orders)
            }
        }
        
        // Observe loading state
        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrderViewModel.LoadingState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is OrderViewModel.LoadingState.Success -> {
                    binding.progressBar.visibility = View.GONE
                }
                is OrderViewModel.LoadingState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun checkAuthAndLoadOrders() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            // Show login prompt
            binding.loginPromptLayout.visibility = View.VISIBLE
            binding.ordersRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
        } else {
            // Load orders
            binding.loginPromptLayout.visibility = View.GONE
            viewModel.loadOrderHistory(currentUser.uid)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload when fragment resumes (e.g., after login)
        checkAuthAndLoadOrders()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
