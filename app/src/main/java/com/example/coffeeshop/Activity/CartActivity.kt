package com.example.coffeeshop.Activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.CartAdapter
import com.example.coffeeshop.Adapter.SelectVoucherAdapter
import com.example.coffeeshop.Helper.BottomNavHelper
import com.example.coffeeshop.Model.RedeemedVoucher
import com.example.coffeeshop.R
import com.example.coffeeshop.Repository.UserRepository
import com.example.coffeeshop.databinding.ActivityCartBinding
import com.example.coffeeshop.databinding.DialogSelectVoucherBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.example.project1762.Helper.ManagmentCart
import com.uilover.project195.Helper.ChangeNumberItemsListener
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var managmentCart: ManagmentCart
    private val userRepository = UserRepository()

    // Voucher state
    private var selectedVoucher: RedeemedVoucher? = null
    private var discountAmount: Double = 0.0
    private var availableVouchers: List<RedeemedVoucher> = emptyList()

    // Phí cố định
    private val percentTax = 0.02
    private val deliveryFee = 0.0       // Miễn phí giao hàng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managmentCart = ManagmentCart(this)

        calculateCart()
        setVariable()
        initCartList()
        loadAvailableVouchers()
        BottomNavHelper.setup(this, BottomNavHelper.Tab.CART)
    }

    override fun onResume() {
        super.onResume()
        // Refresh giỏ hàng mỗi khi quay lại (fix: navbar navigate không gọi onCreate)
        initCartList()
        calculateCart()
        BottomNavHelper.updateCartBadge(this)
    }

    // ─────────────────────────────────────────────────
    //  CART LIST
    // ─────────────────────────────────────────────────

    private fun initCartList() {
        binding.cartView.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.VERTICAL, false
        )
        binding.cartView.adapter = CartAdapter(
            managmentCart.getListCart(),
            this,
            object : ChangeNumberItemsListener {
                override fun onChanged() {
                    calculateCart()
                    updateEmptyState()
                    BottomNavHelper.updateCartBadge(this@CartActivity)
                }
            }
        )
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = managmentCart.getListCart().isEmpty()
        binding.emptyCartLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.cartView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.voucherSection.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.checkoutBtn.isEnabled = !isEmpty
    }

    // ─────────────────────────────────────────────────
    //  VOUCHER
    // ─────────────────────────────────────────────────

    /** Load danh sách voucher khả dụng từ Firestore */
    private fun loadAvailableVouchers() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Log.w("CartActivity", "Chưa đăng nhập, bỏ qua load vouchers")
            return
        }
        lifecycleScope.launch {
            // Thử query có composite index trước
            userRepository.getAvailableVouchers(currentUser.uid)
                .onSuccess { vouchers ->
                    availableVouchers = vouchers
                    Log.d("CartActivity", "✅ Loaded ${vouchers.size} vouchers khả dụng")
                }
                .onFailure { e ->
                    Log.e("CartActivity", "⚠️ Query getAvailableVouchers thất bại: ${e.message}")
                    Log.d("CartActivity", "→ Thử fallback: load tất cả vouchers rồi filter...")
                    // Fallback: load tất cả rồi filter trong bộ nhớ
                    userRepository.getRedeemedVouchers(currentUser.uid)
                        .onSuccess { allVouchers ->
                            availableVouchers = allVouchers.filter { !it.isUsed }
                            Log.d("CartActivity", "✅ Fallback: Loaded ${availableVouchers.size} vouchers khả dụng từ ${allVouchers.size} tổng")
                        }
                        .onFailure { e2 ->
                            Log.e("CartActivity", "❌ Fallback cũng thất bại: ${e2.message}")
                        }
                }
        }
    }

    /** Mở bottom sheet chọn voucher */
    private fun openVoucherDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            AlertDialog.Builder(this)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage("Vui lòng đăng nhập để sử dụng voucher")
                .setPositiveButton("Đăng nhập") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val sheetBinding = DialogSelectVoucherBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Đóng dialog
        sheetBinding.closeVoucherDialog.setOnClickListener { dialog.dismiss() }

        if (availableVouchers.isEmpty()) {
            sheetBinding.emptyVoucherState.visibility = View.VISIBLE
            sheetBinding.voucherRecyclerView.visibility = View.GONE
            sheetBinding.removeVoucherBtn.visibility = View.GONE
        } else {
            sheetBinding.emptyVoucherState.visibility = View.GONE
            sheetBinding.voucherRecyclerView.visibility = View.VISIBLE

            val adapter = SelectVoucherAdapter(
                vouchers = availableVouchers,
                selectedVoucherId = selectedVoucher?.id ?: ""
            ) { voucher ->
                applyVoucher(voucher)
                dialog.dismiss()
            }

            sheetBinding.voucherRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@CartActivity)
                this.adapter = adapter
            }

            // Hiện nút bỏ chọn nếu đang có voucher được chọn
            if (selectedVoucher != null) {
                sheetBinding.removeVoucherBtn.visibility = View.VISIBLE
                sheetBinding.removeVoucherBtn.setOnClickListener {
                    removeVoucher()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    /** Áp dụng voucher đã chọn */
    private fun applyVoucher(voucher: RedeemedVoucher) {
        val subtotal = managmentCart.getTotalFee()
        discountAmount = voucher.calculateDiscount(subtotal)

        selectedVoucher = voucher
        binding.selectVoucherText.text = "${voucher.name} — ${voucher.discountLabel}"
        binding.discountRow.visibility = View.VISIBLE
        binding.discountAmountTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.formatDiscount(discountAmount)

        calculateCart()
    }

    /** Bỏ chọn voucher */
    private fun removeVoucher() {
        selectedVoucher = null
        discountAmount = 0.0
        binding.selectVoucherText.text = "Chọn voucher giảm giá"
        binding.discountRow.visibility = View.GONE
        calculateCart()
    }

    // ─────────────────────────────────────────────────
    //  CALCULATE & DISPLAY
    // ─────────────────────────────────────────────────

    private fun calculateCart() {
        val subtotal = managmentCart.getTotalFee()

        // Tính lại discount nếu là percent (subtotal thay đổi)
        if (selectedVoucher != null) {
            discountAmount = selectedVoucher!!.calculateDiscount(subtotal)
            binding.discountAmountTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.formatDiscount(discountAmount)
        }

        val tax = subtotal * percentTax
        val totalBeforeDiscount = subtotal + tax + deliveryFee
        val total = maxOf(0.0, totalBeforeDiscount - discountAmount)

        binding.totalFeeTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.format(subtotal)
        binding.taxTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.format(tax)
        binding.deliveryTxt.text = "Miễn phí"
        binding.totalTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.format(total)
    }

    // ─────────────────────────────────────────────────
    //  VARIABLE / CLICK LISTENERS
    // ─────────────────────────────────────────────────

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }

        // Nút chọn voucher
        binding.selectVoucherBtn.setOnClickListener { openVoucherDialog() }

        // Nút Thanh toán
        binding.checkoutBtn.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                AlertDialog.Builder(this)
                    .setTitle("Yêu cầu đăng nhập")
                    .setMessage("Vui lòng đăng nhập để tiến hành thanh toán")
                    .setPositiveButton("Đăng nhập") { _, _ ->
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            } else {
                val subtotal = managmentCart.getTotalFee()
                val tax = Math.round(subtotal * percentTax * 100) / 100.0
                val totalBeforeDiscount = subtotal + tax + deliveryFee
                val total = maxOf(0.0, totalBeforeDiscount - discountAmount)

                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putExtra("subtotal", subtotal)
                    putExtra("tax", tax)
                    putExtra("delivery", deliveryFee)
                    putExtra("total", total)
                    putExtra("discountAmount", discountAmount)
                    putExtra("voucherId", selectedVoucher?.id ?: "")
                    putExtra("discountType", selectedVoucher?.discountType ?: "")
                    putExtra("voucherName", selectedVoucher?.name ?: "")
                }
                startActivity(intent)
            }
        }
    }
}