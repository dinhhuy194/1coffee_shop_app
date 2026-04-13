package com.example.coffeeshop.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Model.PaymentRequest
import com.example.coffeeshop.Repository.OrderRepository
import com.example.coffeeshop.Repository.UserRepository
import com.example.coffeeshop.Repository.VnPayApiService
import kotlinx.coroutines.launch

/**
 * ViewModel xử lý logic nghiệp vụ cho màn hình Checkout.
 * Hỗ trợ 2 phương thức thanh toán: COD (tiền mặt) và VNPAY (chuyển khoản online).
 * Hỗ trợ voucher giảm giá: sau khi đặt hàng thành công sẽ đánh dấu voucher là đã dùng.
 */
class CheckoutViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val userRepository = UserRepository()
    private val vnPayApiService = VnPayApiService.create()
    
    private val _checkoutState = MutableLiveData<CheckoutState>(CheckoutState.Idle)
    val checkoutState: LiveData<CheckoutState> = _checkoutState
    
    /**
     * Đặt hàng với phương thức thanh toán COD (tiền mặt khi nhận hàng).
     * Luồng: Tạo Order → Cộng điểm BEAN → Đánh dấu voucher đã dùng → Thông báo thành công
     *
     * @param voucherId      ID của redeemed_voucher người dùng chọn (rỗng nếu không dùng)
     * @param discountAmount Số tiền được giảm (0 nếu không dùng voucher)
     * @param discountType   Loại discount ("fixed", "percent", "free_ship")
     */
    fun placeOrder(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double,
        voucherId: String = "",
        discountAmount: Double = 0.0,
        discountType: String = ""
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            
            val result = repository.createOrder(
                userId = userId,
                items = items,
                subtotal = subtotal,
                tax = tax,
                shippingFee = delivery,
                totalAmount = totalAmount,
                paymentMethod = "COD",
                voucherId = voucherId,
                discountAmount = discountAmount,
                discountType = discountType
            )
            
            if (result.isSuccess) {
                val orderId = result.getOrNull() ?: ""
                
                // Cộng điểm BEAN sau khi đặt hàng thành công
                val orderAmountLong = totalAmount.toLong()
                userRepository.processPointsEarned(userId, orderAmountLong, orderId)
                    .onSuccess { beansEarned ->
                        Log.d("CheckoutVM", "Cộng $beansEarned BEAN cho đơn hàng #$orderId")
                    }
                    .onFailure { e ->
                        Log.e("CheckoutVM", "Lỗi cộng BEAN: ${e.message}")
                    }

                // Đánh dấu voucher đã sử dụng (nếu có)
                if (voucherId.isNotEmpty()) {
                    userRepository.markVoucherUsed(userId, voucherId)
                        .onSuccess {
                            Log.d("CheckoutVM", "✅ Voucher $voucherId đã được đánh dấu sử dụng")
                        }
                        .onFailure { e ->
                            Log.e("CheckoutVM", "❌ Lỗi đánh dấu voucher: ${e.message}")
                        }
                }
                
                _checkoutState.value = CheckoutState.Success(orderId)
            } else {
                _checkoutState.value = CheckoutState.Error(
                    result.exceptionOrNull()?.message ?: "Lỗi đặt hàng"
                )
            }
        }
    }

    /**
     * Đặt hàng với phương thức thanh toán VNPAY.
     * Luồng: Tạo Order (paymentStatus=unpaid) → Gọi API lấy paymentUrl → Trả URL cho Activity
     */
    fun placeOrderWithVnPay(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double,
        voucherId: String = "",
        discountAmount: Double = 0.0,
        discountType: String = ""
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading

            try {
                // Bước 1: Tạo đơn hàng trên Firestore
                val orderResult = repository.createOrder(
                    userId = userId,
                    items = items,
                    subtotal = subtotal,
                    tax = tax,
                    shippingFee = delivery,
                    totalAmount = totalAmount,
                    paymentMethod = "VNPAY",
                    voucherId = voucherId,
                    discountAmount = discountAmount,
                    discountType = discountType
                )

                if (orderResult.isFailure) {
                    _checkoutState.value = CheckoutState.Error(
                        orderResult.exceptionOrNull()?.message ?: "Lỗi tạo đơn hàng"
                    )
                    return@launch
                }

                val orderId = orderResult.getOrNull() ?: ""

                // Đánh dấu voucher đã sử dụng ngay khi tạo order VNPAY
                if (voucherId.isNotEmpty()) {
                    userRepository.markVoucherUsed(userId, voucherId)
                        .onSuccess { Log.d("CheckoutVM", "✅ Voucher $voucherId đánh dấu dùng (VNPAY)") }
                        .onFailure { e -> Log.e("CheckoutVM", "❌ Lỗi voucher VNPAY: ${e.message}") }
                }

                // Bước 2: Tạo request thanh toán (giá đã là VND, không cần quy đổi)
                val paymentRequest = PaymentRequest(
                    orderId = orderId,
                    amount = totalAmount
                )

                val response = vnPayApiService.createPaymentUrl(paymentRequest)

                if (response.paymentUrl.isNotEmpty()) {
                    // Cộng điểm BEAN cho đơn hàng VNPAY
                    val orderAmountLong = totalAmount.toLong()
                    userRepository.processPointsEarned(userId, orderAmountLong, orderId)
                        .onSuccess { beansEarned ->
                            Log.d("CheckoutVM", "Cộng $beansEarned BEAN cho đơn hàng VNPAY #$orderId")
                        }
                        .onFailure { e ->
                            Log.e("CheckoutVM", "Lỗi cộng BEAN: ${e.message}")
                        }

                    _checkoutState.value = CheckoutState.PaymentUrlReady(
                        orderId = orderId,
                        paymentUrl = response.paymentUrl
                    )
                } else {
                    _checkoutState.value = CheckoutState.Error("Không nhận được URL thanh toán từ server")
                }
            } catch (e: Exception) {
                Log.e("CheckoutVM", "Lỗi tạo thanh toán VNPAY: ${e.message}")
                _checkoutState.value = CheckoutState.Error(
                    "Lỗi kết nối server: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Sealed class định nghĩa các trạng thái của quá trình Checkout.
     */
    sealed class CheckoutState {
        object Idle : CheckoutState()
        object Loading : CheckoutState()
        data class Success(val orderId: String) : CheckoutState()
        data class PaymentUrlReady(val orderId: String, val paymentUrl: String) : CheckoutState()
        data class Error(val message: String) : CheckoutState()
    }
}
