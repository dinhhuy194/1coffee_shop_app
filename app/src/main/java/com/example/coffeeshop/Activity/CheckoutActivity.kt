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
 * Activity mÃ n hÃ¬nh Checkout - XÃ¡c nháº­n Ä‘Æ¡n hÃ ng vÃ  chá»n phÆ°Æ¡ng thá»©c thanh toÃ¡n.
 *
 * Luá»“ng Ä‘á»‹a chá»‰ giao hÃ ng (2 cÃ¡ch):
 * 1. Nháº­p vÃ  tÃ¬m kiáº¿m trá»±c tiáº¿p: EditText â†’ debounce â†’ Forward Geocoding â†’ chá»n gá»£i Ã½ â†’ lÆ°u Firestore
 * 2. Chá»n trÃªn báº£n Ä‘á»“: má»Ÿ MapboxPickerActivity â†’ xÃ¡c nháº­n â†’ quay láº¡i â†’ reload Ä‘á»‹a chá»‰
 *
 * Luá»“ng thanh toÃ¡n:
 * - COD: táº¡o order â†’ hoÃ n táº¥t
 * - VNPAY: táº¡o order â†’ má»Ÿ WebView â†’ nháº­n káº¿t quáº£ â†’ cáº­p nháº­t tráº¡ng thÃ¡i
 */
class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()
    private lateinit var managmentCart: ManagmentCart

    private var subtotal = 0.0
    private var tax = 0.0
    private var delivery = 0.0
    private var total = 0.0
    private var discountAmount = 0.0
    private var voucherId = ""
    private var discountType = ""
    private var voucherName = ""

    private var currentOrderId: String = ""
    private val orderRepository = OrderRepository()
    private val db = FirebaseFirestore.getInstance()

    /** Retrofit service cho Mapbox Forward Geocoding (inline search) */
    private val mapboxApiService: MapboxApiService by lazy { MapboxApiService.create() }

    /** Mapbox public token (từ string resource) */
    private lateinit var mapboxToken: String

    /** Danh sÃ¡ch káº¿t quáº£ search gá»£i Ã½ Ä‘á»‹a chá»‰ trong checkout */
    private var checkoutSearchResults: List<MapboxFeature> = emptyList()
    private lateinit var checkoutSuggestionsAdapter: ArrayAdapter<String>

    /** Job debounce cho search inline */
    private var checkoutSearchJob: Job? = null

    /** Äá»‹a chá»‰ hiá»‡n Ä‘ang Ä‘Æ°á»£c lÆ°u (dÃ¹ng Ä‘á»ƒ khÃ´i phá»¥c khi ngÆ°á»i dÃ¹ng thoÃ¡t search mÃ  khÃ´ng chá»n) */
    private var currentSavedAddress: String? = null

    /** Flag tránh TextWatcher trigger khi set text bằng code */
    private var isProgrammaticTextChange = false

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  LAUNCHERS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                    ).onSuccess { Log.d(TAG, "âœ… paymentStatus = paid: $currentOrderId") }
                      .onFailure { e -> Log.e(TAG, "âŒ Lá»—i paymentStatus: ${e.message}") }

                    orderRepository.updateOrderStatus(
                        orderId = currentOrderId,
                        orderStatus = "completed"
                    ).onSuccess { Log.d(TAG, "âœ… orderStatus = completed: $currentOrderId") }
                      .onFailure { e -> Log.e(TAG, "âŒ Lá»—i orderStatus: ${e.message}") }
                }
                showOrderSuccessDialog(currentOrderId, transactionNo)
            }
        } else {
            Toast.makeText(this, "Thanh toÃ¡n Ä‘Ã£ bá»‹ há»§y hoáº·c tháº¥t báº¡i", Toast.LENGTH_SHORT).show()
            binding.placeOrderBtn.isEnabled = true
        }
    }

    /**
     * Launcher nhận kết quả từ MapboxPickerActivity.
     * Khi ngÆ°á»i dÃ¹ng xÃ¡c nháº­n Ä‘á»‹a chá»‰ trÃªn báº£n Ä‘á»“ â†’ reload tá»« Firestore.
     */
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "ðŸ“ Äá»‹a chá»‰ má»›i tá»« báº£n Ä‘á»“, Ä‘ang reload Firestore...")
            hideCheckoutSuggestions()
            loadAddressFromFirestore()
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  LIFECYCLE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managmentCart = ManagmentCart(this)

        // Láº¥y token Mapbox cho inline search
        mapboxToken = getString(R.string.mapbox_access_token)

        subtotal = intent.getDoubleExtra("subtotal", 0.0)
        tax = intent.getDoubleExtra("tax", 0.0)
        delivery = intent.getDoubleExtra("delivery", 0.0)
        total = intent.getDoubleExtra("total", 0.0)
        discountAmount = intent.getDoubleExtra("discountAmount", 0.0)
        voucherId = intent.getStringExtra("voucherId") ?: ""
        discountType = intent.getStringExtra("discountType") ?: ""
        voucherName = intent.getStringExtra("voucherName") ?: ""

        // Khá»Ÿi táº¡o adapter cho checkout suggestions
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  ORDER SUMMARY
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun displayOrderSummary() {
        binding.apply {
            subtotalTxt.text = "${String.format("%,.0f", subtotal)}\u0111"
            taxTxt.text = "${String.format("%,.0f", tax)}\u0111"
            deliveryTxt.text = if (delivery == 0.0) "Miễn phí" else "${String.format("%,.0f", delivery)}đ"
            // Hiển thị dòng discount nếu có voucher
            if (discountAmount > 0 && voucherId.isNotEmpty()) {
                discountRow.visibility = android.view.View.VISIBLE
                discountValueTxt.text = "-${String.format("%,.0f", discountAmount)}\u0111"
                voucherNameTxt.text = if (voucherName.isNotEmpty()) "\uD83C\uDFAB $voucherName" else "\uD83C\uDFAB Voucher giảm giá"
            } else {
                discountRow.visibility = android.view.View.GONE
            }
            totalTxt.text = "${String.format("%,.0f", total)}\u0111"
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  ADDRESS SECTION
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Thiáº¿t láº­p toÃ n bá»™ pháº§n Ä‘á»‹a chá»‰ giao hÃ ng:
     * - Load từ Firestore
     * - NÃºt chá»n/thay Ä‘á»•i báº£n Ä‘á»“
     * - Inline search bar vá»›i autocomplete
     */
    private fun setupAddressSection() {
        binding.selectAddressBtn.setOnClickListener { openMapPicker() }
        setupInlineSearch()
    }

    /**
     * Load Ä‘á»‹a chá»‰ tá»« Firestore users/{userId}.address.
     * Toggle visibility: cÃ³ Ä‘á»‹a chá»‰ â†’ changeBtn; chÆ°a cÃ³ â†’ selectBtn.
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
                    Log.d(TAG, "ðŸ“ Äá»‹a chá»‰ hiá»‡n táº¡i: $address")
                } else {
                    binding.checkoutAddressSearchEdit.setText("")
                }
                isProgrammaticTextChange = false
            } catch (e: Exception) {
                Log.e(TAG, "âŒ Lá»—i load Ä‘á»‹a chá»‰ Firestore: ${e.message}")
            }
        }
    }

    /**
     * Thiáº¿t láº­p inline search bar trong trang Checkout.
     *
     * Luá»“ng: Nháº­p text â†’ debounce 500ms â†’ forwardGeocode â†’ hiá»ƒn thá»‹ suggestions â†’
     *         ngÆ°á»i dÃ¹ng chá»n â†’ lÆ°u tháº³ng vÃ o Firestore â†’ cáº­p nháº­t addressTxt
     */
    private fun setupInlineSearch() {
        // Khi ngÆ°á»i dÃ¹ng focus vÃ o field â†’ xÃ³a text Ä‘á»ƒ gÃµ tÃ¬m kiáº¿m
        binding.checkoutAddressSearchEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isProgrammaticTextChange = true
                binding.checkoutAddressSearchEdit.setText("")
                isProgrammaticTextChange = false
                binding.checkoutAddressSearchEdit.hint = "Nháº­p Ä‘á»‹a chá»‰ Ä‘á»ƒ tÃ¬m kiáº¿m..."
            } else {
                // Máº¥t focus mÃ  khÃ´ng chá»n gá»£i Ã½ â†’ khÃ´i phá»¥c Ä‘á»‹a chá»‰ Ä‘Ã£ lÆ°u
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
                    binding.checkoutAddressSearchEdit.hint = "Vui lÃ²ng chá»n Ä‘á»‹a chá»‰ giao hÃ ng"
                }
                isProgrammaticTextChange = false
            }
        }

        // TextWatcher vá»›i debounce cho inline search
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

        // Báº¥m Enter/Search trÃªn bÃ n phÃ­m â†’ tÃ¬m ngay
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

        // NÃºt xÃ³a text (chá»‰ hiá»‡n khi Ä‘ang gÃµ tÃ¬m kiáº¿m)
        binding.checkoutClearSearchBtn.setOnClickListener {
            binding.checkoutAddressSearchEdit.setText("")
            hideCheckoutSuggestions()
        }

        // Chá»n má»™t gá»£i Ã½ â†’ lÆ°u Firestore ngay â†’ cáº­p nháº­t field
        binding.checkoutSuggestionsList.setOnItemClickListener { _, _, position, _ ->
            val selected = checkoutSearchResults.getOrNull(position) ?: return@setOnItemClickListener
            val address = selected.properties?.full_address
                ?: selected.properties?.name
                ?: "Äá»‹a chá»‰ khÃ´ng xÃ¡c Ä‘á»‹nh"

            Log.d(TAG, "âœ… Chá»n Ä‘á»‹a chá»‰ tá»« gá»£i Ã½: $address")

            saveAddressFromSearch(address)
            hideCheckoutSuggestions()
            hideKeyboard()
            binding.checkoutAddressSearchEdit.clearFocus()
        }
    }

    /**
     * Gá»i Forward Geocoding Ä‘á»ƒ tÃ¬m gá»£i Ã½ Ä‘á»‹a chá»‰ inline trong Checkout.
     */
    private suspend fun checkoutForwardGeocode(query: String) {
        if (mapboxToken.isBlank()) {
            Log.w(TAG, "âš ï¸ Mapbox token trá»‘ng, bá» qua inline search")
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
                        ?: "Äá»‹a chá»‰ khÃ´ng xÃ¡c Ä‘á»‹nh"
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
            Log.e(TAG, "âŒ Lá»—i checkout forward geocoding: ${e.message}")
            runOnUiThread { hideCheckoutSuggestions() }
        } finally {
            runOnUiThread { binding.checkoutSearchProgress.visibility = View.GONE }
        }
    }

    /**
     * LÆ°u Ä‘á»‹a chá»‰ chá»n tá»« inline search vÃ o Firestore.
     * Cập nhật UI addressTxt ngay sau khi lưu thành công.
     *
     * @param address Chuá»—i Ä‘á»‹a chá»‰ Ä‘áº§y Ä‘á»§ tá»« gá»£i Ã½
     */
    private fun saveAddressFromSearch(address: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        lifecycleScope.launch {
            try {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("address", address)
                    .await()

                Log.d(TAG, "âœ… LÆ°u Ä‘á»‹a chá»‰ inline search: $address")

                // Cập nhật trạng thái và UI ngay lập tức
                currentSavedAddress = address
                isProgrammaticTextChange = true
                binding.checkoutAddressSearchEdit.setText(address)
                binding.checkoutAddressSearchEdit.setTextColor(getColor(R.color.darkBrown))
                isProgrammaticTextChange = false

                Toast.makeText(this@CheckoutActivity, "âœ… ÄÃ£ lÆ°u Ä‘á»‹a chá»‰!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "âŒ Lá»—i lÆ°u Ä‘á»‹a chá»‰ tá»« search: ${e.message}")
                Toast.makeText(this@CheckoutActivity, "Lá»—i lÆ°u Ä‘á»‹a chá»‰. Thá»­ láº¡i.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Má»Ÿ MapboxPickerActivity Ä‘á»ƒ chá»n Ä‘á»‹a chá»‰ báº±ng báº£n Ä‘á»“. */
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  PLACE ORDER BUTTON
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun setupPlaceOrderButton() {
        binding.placeOrderBtn.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Báº¡n cáº§n Ä‘Äƒng nháº­p Ä‘á»ƒ Ä‘áº·t hÃ ng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cartItems = managmentCart.getListCart()

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Giá» hÃ ng trá»‘ng", Toast.LENGTH_SHORT).show()
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
                        totalAmount = total,
                        voucherId = voucherId,
                        discountAmount = discountAmount,
                        discountType = discountType
                    )
                }
                else -> {
                    viewModel.placeOrder(
                        userId = currentUser.uid,
                        items = cartItems,
                        subtotal = subtotal,
                        tax = tax,
                        delivery = delivery,
                        totalAmount = total,
                        voucherId = voucherId,
                        discountAmount = discountAmount,
                        discountType = discountType
                    )
                }
            }
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  CHECKOUT STATE OBSERVER
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                    Toast.makeText(this, "Lá»—i: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  SUCCESS DIALOG
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun showOrderSuccessDialog(orderId: String, transactionNo: String) {
        AlertDialog.Builder(this)
            .setTitle("ðŸŽ‰ Äáº·t hÃ ng thÃ nh cÃ´ng!")
            .setMessage(
                "ÄÆ¡n hÃ ng cá»§a báº¡n Ä‘Ã£ Ä‘Æ°á»£c xÃ¡c nháº­n.\n\n" +
                "ðŸ“‹ MÃ£ Ä‘Æ¡n hÃ ng:\n$orderId\n\n" +
                "ðŸ’³ MÃ£ giao dá»‹ch VNPAY:\n$transactionNo\n\n" +
                "Cáº£m Æ¡n báº¡n Ä‘Ã£ mua hÃ ng! â˜•"
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
