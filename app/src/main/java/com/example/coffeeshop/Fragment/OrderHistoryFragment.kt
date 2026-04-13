package com.example.coffeeshop.Fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Activity.CartActivity
import com.example.coffeeshop.Activity.LoginActivity
import com.example.coffeeshop.Activity.VnPayWebViewActivity
import com.example.coffeeshop.Adapter.OrderAdapter
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Model.PaymentRequest
import com.example.coffeeshop.Repository.OrderRepository
import com.example.coffeeshop.Repository.VnPayApiService
import com.example.coffeeshop.ViewModel.OrderViewModel
import com.example.coffeeshop.databinding.FragmentOrderHistoryBinding
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị lịch sử đơn hàng.
 *
 * Bao gồm:
 * - Danh sách đơn hàng với trạng thái (orderStatus + paymentStatus)
 * - Nút "Thanh toán ngay" cho đơn VNPAY chưa thanh toán
 * - Filter/Sort
 * - Swipe-to-refresh
 */
class OrderHistoryFragment : Fragment() {
    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderViewModel by viewModels()
    
    private var currentFilterOptions: OrderFilterOptions = OrderFilterOptions()
    private var allOrders: List<Order> = emptyList()

    /** Đơn hàng đang thanh toán (dùng để cập nhật sau khi WebView trả kết quả) */
    private var pendingPaymentOrder: Order? = null
    private val orderRepository = OrderRepository()
    private val vnPayApiService = VnPayApiService.create()

    /**
     * Launcher nhận kết quả từ VnPayWebViewActivity.
     * Khi thanh toán thành công → update Firestore (paymentStatus + orderStatus) → refresh danh sách.
     */
    private val vnPayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val order = pendingPaymentOrder ?: return@registerForActivityResult

        if (result.resultCode == Activity.RESULT_OK) {
            val isSuccess = result.data?.getBooleanExtra(
                VnPayWebViewActivity.RESULT_PAYMENT_SUCCESS, false
            ) ?: false

            if (isSuccess) {
                val transactionNo = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_TRANSACTION_NO) ?: ""
                val bankCode = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_BANK_CODE) ?: ""
                val payDate = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_PAY_DATE) ?: ""

                // Cập nhật Firestore: paymentStatus = "paid" + orderStatus = "completed"
                lifecycleScope.launch {
                    // Bước 1: Cập nhật trạng thái thanh toán
                    orderRepository.updatePaymentStatus(
                        orderId = order.orderId,
                        paymentStatus = "paid",
                        transactionNo = transactionNo,
                        bankCode = bankCode,
                        payDate = payDate
                    ).onSuccess {
                        Log.d("OrderHistory", "✅ Đã cập nhật paymentStatus = paid cho: ${order.orderId}")
                    }

                    // Bước 2: Cập nhật trạng thái đơn hàng → completed
                    orderRepository.updateOrderStatus(
                        orderId = order.orderId,
                        orderStatus = "completed"
                    ).onSuccess {
                        Log.d("OrderHistory", "✅ Đã cập nhật orderStatus = completed cho: ${order.orderId}")
                    }

                    // Bước 3: Refresh danh sách đơn hàng
                    Toast.makeText(requireContext(), "Thanh toán thành công! 🎉 Đơn hàng đã hoàn tất.", Toast.LENGTH_LONG).show()
                    checkAuthAndLoadOrders()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Thanh toán đã bị hủy hoặc thất bại", Toast.LENGTH_SHORT).show()
        }

        pendingPaymentOrder = null
    }
    
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
        // Lọc theo khoảng thời gian
        val filteredByDate = filterByDateRange(allOrders, currentFilterOptions.dateRange)
        
        // Sắp xếp
        val sortedOrders = when (currentFilterOptions.sortType) {
            OrderSortType.DATE_NEWEST -> filteredByDate.sortedByDescending { it.timestamp }
            OrderSortType.DATE_OLDEST -> filteredByDate.sortedBy { it.timestamp }
            OrderSortType.PRICE_HIGH_LOW -> filteredByDate.sortedByDescending { it.totalAmount }
            OrderSortType.PRICE_LOW_HIGH -> filteredByDate.sortedBy { it.totalAmount }
        }
        
        // Truyền callbacks vào adapter
        binding.ordersRecyclerView.adapter = OrderAdapter(
            orders = sortedOrders,
            onPayNowClick = { order -> handlePayNow(order) },
            onReorderClick = { order -> handleReorder(order) }
        )
    }

    /**
     * Xử lý khi user bấm "Mua lại" trên đơn hàng.
     * 
     * Luồng:
     * 1. Convert OrderItem list → ItemsModel list
     * 2. Clear giỏ hàng hiện tại
     * 3. Thêm tất cả items vào giỏ hàng mới
     * 4. Điều hướng tới CartActivity
     */
    private fun handleReorder(order: Order) {
        val managmentCart = ManagmentCart(requireContext())
        
        // Convert OrderItem → ItemsModel
        val cartItems = ArrayList<ItemsModel>()
        for (orderItem in order.items) {
            val picUrls = ArrayList<String>()
            if (orderItem.imageUrl.isNotEmpty()) {
                picUrls.add(orderItem.imageUrl)
            }
            val item = ItemsModel(
                title = orderItem.title,
                price = orderItem.price,
                numberInCart = orderItem.quantity,
                picUrl = picUrls,
                selectedSize = orderItem.selectedSize.ifEmpty { "Medium" },
                iceOption = orderItem.iceOption.ifEmpty { "Đá chung" },
                sugarOption = orderItem.sugarOption.ifEmpty { "Bình thường" }
            )
            cartItems.add(item)
        }
        
        // Replace cart với items từ đơn cũ
        managmentCart.replaceCart(cartItems)
        
        Toast.makeText(requireContext(), "Đã thêm ${order.items.size} sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        
        // Navigate to CartActivity
        val intent = Intent(requireContext(), CartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    /**
     * Xử lý khi user bấm "Thanh toán ngay" trên đơn hàng pending.
     *
     * Luồng:
     * 1. Lưu order hiện tại → pendingPaymentOrder
     * 2. Quy đổi USD → VND
     * 3. Gọi API Backend tạo paymentUrl
     * 4. Mở VnPayWebViewActivity
     * 5. Nhận kết quả → update Firestore (trong vnPayLauncher)
     */
    private fun handlePayNow(order: Order) {
        pendingPaymentOrder = order

        lifecycleScope.launch {
            try {
                Toast.makeText(requireContext(), "Đang tạo liên kết thanh toán...", Toast.LENGTH_SHORT).show()

                // Giá đã là VND, không cần quy đổi
                val paymentRequest = PaymentRequest(
                    orderId = order.orderId,
                    amount = order.totalAmount
                )

                val response = vnPayApiService.createPaymentUrl(paymentRequest)

                if (response.paymentUrl.isNotEmpty()) {
                    // Mở WebView thanh toán
                    val webViewIntent = Intent(requireContext(), VnPayWebViewActivity::class.java).apply {
                        putExtra(VnPayWebViewActivity.EXTRA_PAYMENT_URL, response.paymentUrl)
                        putExtra(VnPayWebViewActivity.EXTRA_ORDER_ID, order.orderId)
                    }
                    vnPayLauncher.launch(webViewIntent)
                } else {
                    Toast.makeText(requireContext(), "Không nhận được URL thanh toán từ server", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OrderHistory", "Lỗi tạo thanh toán: ${e.message}")
                Toast.makeText(requireContext(), "Lỗi kết nối server: ${e.message}", Toast.LENGTH_LONG).show()
                pendingPaymentOrder = null
            }
        }
    }
    
    private fun filterByDateRange(orders: List<Order>, dateRange: DateRangeFilter): List<Order> {
        if (dateRange == DateRangeFilter.ALL_TIME) return orders
        
        val calendar = java.util.Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
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
        // Quan sát danh sách đơn hàng
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            binding.swipeRefreshLayout.isRefreshing = false
            allOrders = orders
            
            if (orders.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.swipeRefreshLayout.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.swipeRefreshLayout.visibility = View.VISIBLE
                applyFiltering()
            }
        }
        
        // Quan sát trạng thái loading
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
            binding.loginPromptLayout.visibility = View.VISIBLE
            binding.swipeRefreshLayout.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        } else {
            binding.loginPromptLayout.visibility = View.GONE
            viewModel.loadOrderHistory(currentUser.uid)
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkAuthAndLoadOrders()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
