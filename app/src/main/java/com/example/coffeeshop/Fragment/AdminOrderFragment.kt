package com.example.coffeeshop.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.AdminOrderAdapter
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.AdminViewModel
import com.example.coffeeshop.databinding.FragmentAdminOrdersBinding

class AdminOrderFragment : Fragment() {
    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminOrderAdapter

    private var allOrders = listOf<Order>()
    private var currentFilter: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminOrderAdapter { orderId, newStatus ->
            viewModel.updateOrderStatus(orderId, newStatus)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Filter chips
        binding.chipAll.setOnClickListener { currentFilter = null; filterOrders() }
        binding.chipPending.setOnClickListener { currentFilter = Order.STATUS_PENDING; filterOrders() }
        binding.chipPreparing.setOnClickListener { currentFilter = Order.STATUS_PREPARING; filterOrders() }
        binding.chipReady.setOnClickListener { currentFilter = Order.STATUS_READY; filterOrders() }
        binding.chipCompleted.setOnClickListener { currentFilter = Order.STATUS_COMPLETED; filterOrders() }
        binding.chipCancelled.setOnClickListener { currentFilter = Order.STATUS_CANCELLED; filterOrders() }

        observeViewModel()
        viewModel.loadOrders()
    }

    private fun observeViewModel() {
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            allOrders = orders
            filterOrders()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun filterOrders() {
        val filtered = if (currentFilter == null) allOrders
        else allOrders.filter { it.orderStatus == currentFilter }

        adapter.updateData(filtered)
        binding.emptyTxt.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
