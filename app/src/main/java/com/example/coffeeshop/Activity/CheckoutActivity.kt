package com.example.coffeeshop.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coffeeshop.Api.MapboxApiService
import com.example.coffeeshop.Model.MapboxFeature
import com.example.coffeeshop.Repository.OrderRepository
import com.example.coffeeshop.ViewModel.CheckoutViewModel
import com.example.coffeeshop.databinding.ActivityCheckoutBinding
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.coffeeshop.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity màn hình Checkout - Xác nhận đơn hàng và chọn phương thức thanh toán.
 *
 * Luồng địa chỉ giao hàng (2 cách):
 * 1. Nhập và tìm kiếm trực tiếp: EditText → debounce → Forward Geocoding → chọn gợi ý → lưu Firestore
 * 2. Chọn trên bản đồ: mở MapboxPickerActivity → xác nhận → quay lại → reload địa chỉ
 *
 * Luồng thanh toán:
 * - COD: tạo order → hoàn tất
 * - VNPAY: tạo order → mở WebView → nhận kết quả → cập nhật trạng thái
 */
class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()
    private lateinit var managmentCart: ManagmentCart

    private var subtotal = 0.0
    private var tax = 0.0
    private var delivery = 0.0
    private var total = 0.0

    private var currentOrderId: String = ""
    private val orderRepository = OrderRepository()
    private val db = FirebaseFirestore.getInstance()

    /** Retrofit service cho Mapbox Forward Geocoding (inline search) */
    private val mapboxApiService: MapboxApiService by lazy { MapboxApiService.create() }

    /** Mapbox public token (từ string resource) */
    private lateinit var mapboxToken: String

    /** Danh sách kết quả search gợi ý địa chỉ trong checkout */
    private var checkoutSearchResults: List<MapboxFeature> = emptyList()
    private lateinit var checkoutSuggestionsAdapter: ArrayAdapter<String>

    /** Job debounce cho search inline */
    private var checkoutSearchJob: Job? = null

    /** Địa chỉ hiện đang được lưu (dùng để khôi phục khi người dùng thoát search mà không chọn) */
    private var currentSavedAddress: String? = null

    /** Flag tránh TextWatcher trigger khi set text bằng code */
    private var isProgrammaticTextChange = false

    // ─────────────────────────────────────────────────
    //  LAUNCHERS
    // ─────────────────────────────────────────────────

    /**
     * Launcher nhận kết quả từ VnPayWebViewActivity.
     */
    private val vnPayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isSuccess = result.data?.getBooleanExtra(
                VnPayWebViewActivity.RESULT_PAYMENT_SUCCESS, false
            ) ?: false

            if (isSuccess) {
                val transactionNo = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_TRANSACTION_NO) ?: ""
                val bankCode = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_BANK_CODE) ?: ""
                val payDate = result.data?.getStringExtra(VnPayWebViewActivity.RESULT_PAY_DATE) ?: ""

                lifecycleScope.launch {
                    orderRepository.updatePaymentStatus(
                        orderId = currentOrderId,
                        paymentStatus = "paid",
                        transactionNo = transactionNo,
                        bankCode = bankCode,
                        payDate = payDate
                    ).onSuccess { Log.d(TAG, "✅ paymentStatus = paid: $currentOrderId") }
                      .onFailure { e -> Log.e(TAG, "❌ Lỗi paymentStatus: ${e.message}") }

                    orderRepository.updateOrderStatus(
                        orderId = currentOrderId,
                        orderStatus = "completed"
                    ).onSuccess { Log.d(TAG, "✅ orderStatus = completed: $currentOrderId") }
                      .onFailure { e -> Log.e(TAG, "❌ Lỗi orderStatus: ${e.message}") }
                }
                showOrderSuccessDialog(currentOrderId, transactionNo)
            }
        } else {
            Toast.makeText(this, "Thanh toán đã bị hủy hoặc thất bại", Toast.LENGTH_SHORT).show()
            binding.placeOrderBtn.isEnabled = true
        }
    }

    /**
     * Launcher nhận kết quả từ MapboxPickerActivity.
     * Khi người dùng xác nhận địa chỉ trên bản đồ → reload từ Firestore.
     */
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "📍 Địa chỉ mới từ bản đồ, đang reload Firestore...")
            hideCheckoutSuggestions()
            loadAddressFromFirestore()
        }
    }

    // ─────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managmentCart = ManagmentCart(this)

        // Lấy token Mapbox cho inline search
        mapboxToken = getString(R.string.mapbox_access_token)

        subtotal = intent.getDoubleExtra("subtotal", 0.0)
        tax = intent.getDoubleExtra("tax", 0.0)
        delivery = intent.getDoubleExtra("delivery", 0.0)
        total = intent.getDoubleExtra("total", 0.0)

        // Khởi tạo adapter cho checkout suggestions
        checkoutSuggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.checkoutSuggestionsList.adapter = checkoutSuggestionsAdapter

        displayOrderSummary()
        setupAddressSection()
        setupPlaceOrderButton()
        observeCheckoutState()

        loadAddressFromFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkoutSearchJob?.cancel()
    }

    // ─────────────────────────────────────────────────
    //  ORDER SUMMARY
    // ─────────────────────────────────────────────────

    private fun displayOrderSummary() {
        binding.apply {
            subtotalTxt.text = "$${String.format("%.2f", subtotal)}"
            taxTxt.text = "$${String.format("%.2f", tax)}"
            deliveryTxt.text = "$${String.format("%.2f", delivery)}"
            totalTxt.text = "$${String.format("%.2f", total)}"
        }
    }

    // ─────────────────────────────────────────────────
    //  ADDRESS SECTION
    // ─────────────────────────────────────────────────

    /**
     * Thiết lập toàn bộ phần địa chỉ giao hàng:
     * - Load từ Firestore
     * - Nút chọn/thay đổi bản đồ
     * - Inline search bar với autocomplete
     */
    private fun setupAddressSection() {
        binding.selectAddressBtn.setOnClickListener { openMapPicker() }
        setupInlineSearch()
    }

    /**
     * Load địa chỉ từ Firestore users/{userId}.address.
     * Toggle visibility: có địa chỉ → changeBtn; chưa có → selectBtn.
     */
    private fun loadAddressFromFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val address = snapshot.getString("address")
                currentSavedAddress = address

                isProgrammaticTextChange = true
                if (!address.isNullOrBlank()) {
                    binding.checkoutAddressSearchEdit.setText(address)
                    binding.checkoutAddressSearchEdit.setTextColor(getColor(R.color.darkBrown))
                    Log.d(TAG, "📍 Địa chỉ hiện tại: $address")
                } else {
                    binding.checkoutAddressSearchEdit.setText("")
                }
                isProgrammaticTextChange = false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi load địa chỉ Firestore: ${e.message}")
            }
        }
    }

    /**
     * Thiết lập inline search bar trong trang Checkout.
     *
     * Luồng: Nhập text → debounce 500ms → forwardGeocode → hiển thị suggestions →
     *         người dùng chọn → lưu thẳng vào Firestore → cập nhật addressTxt
     */
    private fun setupInlineSearch() {
        // Khi người dùng focus vào field → xóa text để gõ tìm kiếm
        binding.checkoutAddressSearchEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isProgrammaticTextChange = true
                binding.checkoutAddressSearchEdit.setText("")
                isProgrammaticTextChange = false
                binding.checkoutAddressSearchEdit.hint = "Nhập địa chỉ để tìm kiếm..."
            } else {
                // Mất focus mà không chọn gợi ý → khôi phục địa chỉ đã lưu
                checkoutSearchJob?.cancel()
                hideCheckoutSuggestions()
                binding.checkoutClearSearchBtn.visibility = View.GONE
                val savedAddress = currentSavedAddress
                isProgrammaticTextChange = true
                if (!savedAddress.isNullOrBlank()) {
                    binding.checkoutAddressSearchEdit.setText(savedAddress)
                    binding.checkoutAddressSearchEdit.setTextColor(getColor(R.color.darkBrown))
                } else {
                    binding.checkoutAddressSearchEdit.setText("")
                    binding.checkoutAddressSearchEdit.hint = "Vui lòng chọn địa chỉ giao hàng"
                }
                isProgrammaticTextChange = false
            }
        }

        // TextWatcher với debounce cho inline search
        binding.checkoutAddressSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChange) return
                val query = s?.toString()?.trim() ?: ""

                binding.checkoutClearSearchBtn.visibility =
                    if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.length < 3) {
                    hideCheckoutSuggestions()
                    return
                }

                checkoutSearchJob?.cancel()
                checkoutSearchJob = lifecycleScope.launch {
                    delay(500L)
                    checkoutForwardGeocode(query)
                }
            }
        })

        // Bấm Enter/Search trên bàn phím → tìm ngay
        binding.checkoutAddressSearchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.checkoutAddressSearchEdit.text.toString().trim()
                if (query.isNotBlank()) {
                    checkoutSearchJob?.cancel()
                    checkoutSearchJob = lifecycleScope.launch { checkoutForwardGeocode(query) }
                }
                true
            } else false
        }

        // Nút xóa text (chỉ hiện khi đang gõ tìm kiếm)
        binding.checkoutClearSearchBtn.setOnClickListener {
            binding.checkoutAddressSearchEdit.setText("")
            hideCheckoutSuggestions()
        }

        // Chọn một gợi ý → lưu Firestore ngay → cập nhật field
        binding.checkoutSuggestionsList.setOnItemClickListener { _, _, position, _ ->
            val selected = checkoutSearchResults.getOrNull(position) ?: return@setOnItemClickListener
            val address = selected.properties?.full_address
                ?: selected.properties?.name
                ?: "Địa chỉ không xác định"

            Log.d(TAG, "✅ Chọn địa chỉ từ gợi ý: $address")

            saveAddressFromSearch(address)
            hideCheckoutSuggestions()
            hideKeyboard()
            binding.checkoutAddressSearchEdit.clearFocus()
        }
    }

    /**
     * Gọi Forward Geocoding để tìm gợi ý địa chỉ inline trong Checkout.
     */
    private suspend fun checkoutForwardGeocode(query: String) {
        if (mapboxToken.isBlank()) {
            Log.w(TAG, "⚠️ Mapbox token trống, bỏ qua inline search")
            return
        }

        binding.checkoutSearchProgress.visibility = View.VISIBLE

        try {
            val response = mapboxApiService.forwardGeocode(
                query = query,
                accessToken = mapboxToken,
                limit = 5
            )

            val features = response.features
            if (!features.isNullOrEmpty()) {
                checkoutSearchResults = features

                val labels = features.map { feature ->
                    feature.properties?.full_address
                        ?: feature.properties?.name
                        ?: "Địa chỉ không xác định"
                }

                runOnUiThread {
                    checkoutSuggestionsAdapter.clear()
                    checkoutSuggestionsAdapter.addAll(labels)
                    checkoutSuggestionsAdapter.notifyDataSetChanged()
                    binding.checkoutSuggestionsList.visibility = View.VISIBLE
                }
            } else {
                runOnUiThread { hideCheckoutSuggestions() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi checkout forward geocoding: ${e.message}")
            runOnUiThread { hideCheckoutSuggestions() }
        } finally {
            runOnUiThread { binding.checkoutSearchProgress.visibility = View.GONE }
        }
    }

    /**
     * Lưu địa chỉ chọn từ inline search vào Firestore.
     * Cập nhật UI addressTxt ngay sau khi lưu thành công.
     *
     * @param address Chuỗi địa chỉ đầy đủ từ gợi ý
     */
    private fun saveAddressFromSearch(address: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        lifecycleScope.launch {
            try {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("address", address)
                    .await()

                Log.d(TAG, "✅ Lưu địa chỉ inline search: $address")

                // Cập nhật trạng thái và UI ngay lập tức
                currentSavedAddress = address
                isProgrammaticTextChange = true
                binding.checkoutAddressSearchEdit.setText(address)
                binding.checkoutAddressSearchEdit.setTextColor(getColor(R.color.darkBrown))
                isProgrammaticTextChange = false

                Toast.makeText(this@CheckoutActivity, "✅ Đã lưu địa chỉ!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi lưu địa chỉ từ search: ${e.message}")
                Toast.makeText(this@CheckoutActivity, "Lỗi lưu địa chỉ. Thử lại.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Mở MapboxPickerActivity để chọn địa chỉ bằng bản đồ. */
    private fun openMapPicker() {
        val intent = Intent(this, MapboxPickerActivity::class.java)
        mapPickerLauncher.launch(intent)
    }

    private fun hideCheckoutSuggestions() {
        checkoutSearchResults = emptyList()
        checkoutSuggestionsAdapter.clear()
        binding.checkoutSuggestionsList.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.checkoutAddressSearchEdit.windowToken, 0)
    }

    // ─────────────────────────────────────────────────
    //  PLACE ORDER BUTTON
    // ─────────────────────────────────────────────────

    private fun setupPlaceOrderButton() {
        binding.placeOrderBtn.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Bạn cần đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cartItems = managmentCart.getListCart()

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            when (binding.paymentMethodGroup.checkedRadioButtonId) {
                R.id.vnpayRadio -> {
                    viewModel.placeOrderWithVnPay(
                        userId = currentUser.uid,
                        items = cartItems,
                        subtotal = subtotal,
                        tax = tax,
                        delivery = delivery,
                        totalAmount = total
                    )
                }
                else -> {
                    viewModel.placeOrder(
                        userId = currentUser.uid,
                        items = cartItems,
                        subtotal = subtotal,
                        tax = tax,
                        delivery = delivery,
                        totalAmount = total
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  CHECKOUT STATE OBSERVER
    // ─────────────────────────────────────────────────

    private fun observeCheckoutState() {
        viewModel.checkoutState.observe(this) { state ->
            when (state) {
                is CheckoutViewModel.CheckoutState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is CheckoutViewModel.CheckoutState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.placeOrderBtn.isEnabled = false
                }
                is CheckoutViewModel.CheckoutState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.placeOrderBtn.isEnabled = true
                    Toast.makeText(this, "Đặt hàng thành công! Mã: ${state.orderId}", Toast.LENGTH_LONG).show()
                    managmentCart.clearCart()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
                is CheckoutViewModel.CheckoutState.PaymentUrlReady -> {
                    currentOrderId = state.orderId
                    binding.progressBar.visibility = View.GONE
                    val webViewIntent = Intent(this, VnPayWebViewActivity::class.java).apply {
                        putExtra(VnPayWebViewActivity.EXTRA_PAYMENT_URL, state.paymentUrl)
                        putExtra(VnPayWebViewActivity.EXTRA_ORDER_ID, state.orderId)
                    }
                    vnPayLauncher.launch(webViewIntent)
                }
                is CheckoutViewModel.CheckoutState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.placeOrderBtn.isEnabled = true
                    Toast.makeText(this, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  SUCCESS DIALOG
    // ─────────────────────────────────────────────────

    private fun showOrderSuccessDialog(orderId: String, transactionNo: String) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Đặt hàng thành công!")
            .setMessage(
                "Đơn hàng của bạn đã được xác nhận.\n\n" +
                "📋 Mã đơn hàng:\n$orderId\n\n" +
                "💳 Mã giao dịch VNPAY:\n$transactionNo\n\n" +
                "Cảm ơn bạn đã mua hàng! ☕"
            )
            .setCancelable(false)
            .setPositiveButton("Về trang chủ") { _, _ ->
                managmentCart.clearCart()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .show()
    }

    companion object {
        private const val TAG = "CheckoutActivity"
    }
}
