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
    
    private var currentFilterOptions: OrderFilterOptions = OrderFilterOptions()
    private var allOrders: List<com.example.coffeeshop.Model.Order> = emptyList()
    
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
        setupSwipeRefresh()
        setupListeners()
        observeViewModel()
        checkAuthAndLoadOrders()
    }
    
    private fun setupRecyclerView() {
        binding.ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            checkAuthAndLoadOrders()
        }
        // Set colors for refresh indicator
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_orange_dark,
            android.R.color.holo_red_dark,
            android.R.color.holo_blue_dark
        )
    }
    
    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            requireActivity().finish()
        }
        
        binding.loginBtn.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        
        binding.filterBtn.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun showFilterBottomSheet() {
        val bottomSheet = OrderFilterBottomSheet(currentFilterOptions) { selectedOptions ->
            currentFilterOptions = selectedOptions
            applyFiltering()
        }
        bottomSheet.show(parentFragmentManager, "OrderFilterBottomSheet")
    }
    
    private fun applyFiltering() {
        // First filter by date range
        val filteredByDate = filterByDateRange(allOrders, currentFilterOptions.dateRange)
        
        // Then apply sorting
        val sortedOrders = when (currentFilterOptions.sortType) {
            OrderSortType.DATE_NEWEST -> filteredByDate.sortedByDescending { it.timestamp }
            OrderSortType.DATE_OLDEST -> filteredByDate.sortedBy { it.timestamp }
            OrderSortType.PRICE_HIGH_LOW -> filteredByDate.sortedByDescending { it.totalAmount }
            OrderSortType.PRICE_LOW_HIGH -> filteredByDate.sortedBy { it.totalAmount }

        }
        
        binding.ordersRecyclerView.adapter = OrderAdapter(sortedOrders)
    }
    
    private fun filterByDateRange(orders: List<com.example.coffeeshop.Model.Order>, dateRange: DateRangeFilter): List<com.example.coffeeshop.Model.Order> {
        if (dateRange == DateRangeFilter.ALL_TIME) return orders
        
        val calendar = java.util.Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        // Calculate start time based on filter
        val startTime = when (dateRange) {
            DateRangeFilter.TODAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateRangeFilter.LAST_7_DAYS -> {
                currentTime - (7 * 24 * 60 * 60 * 1000L)
            }
            DateRangeFilter.LAST_30_DAYS -> {
                currentTime - (30 * 24 * 60 * 60 * 1000L)
            }
            else -> 0L
        }
        
        return orders.filter { it.timestamp >= startTime }
    }
    
    private fun observeViewModel() {
        // Observe orders
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            binding.swipeRefreshLayout.isRefreshing = false
            allOrders = orders  // Store all orders
            
            if (orders.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.swipeRefreshLayout.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.swipeRefreshLayout.visibility = View.VISIBLE
                applyFiltering()  // Apply current filters
            }
        }
        
        // Observe loading state
        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrderViewModel.LoadingState.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is OrderViewModel.LoadingState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is OrderViewModel.LoadingState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "Lỗi: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }
    
    private fun checkAuthAndLoadOrders() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            // Show login prompt
            binding.loginPromptLayout.visibility = View.VISIBLE
            binding.swipeRefreshLayout.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
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
