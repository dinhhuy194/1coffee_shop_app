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
import com.example.coffeeshop.Repository.ExchangeRateService
import kotlinx.coroutines.launch

/**
 * ViewModel xử lý logic nghiệp vụ cho màn hình Checkout.
 * Hỗ trợ 2 phương thức thanh toán: COD (tiền mặt) và VNPAY (chuyển khoản online).
 */
class CheckoutViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val userRepository = UserRepository()
    private val vnPayApiService = VnPayApiService.create()
    
    private val _checkoutState = MutableLiveData<CheckoutState>(CheckoutState.Idle)
    val checkoutState: LiveData<CheckoutState> = _checkoutState
    
    /**
     * Đặt hàng với phương thức thanh toán COD (tiền mặt khi nhận hàng).
     * Luồng: Tạo Order → Cộng điểm BEAN → Thông báo thành công
     */
    fun placeOrder(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double
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
                paymentMethod = "COD"
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
                
                _checkoutState.value = CheckoutState.Success(orderId)
            } else {
                _checkoutState.value = CheckoutState.Error(
                    result.exceptionOrNull()?.message ?: "Order failed"
                )
            }
        }
    }

    /**
     * Đặt hàng với phương thức thanh toán VNPAY.
     * Luồng: Tạo Order (paymentStatus=unpaid) → Gọi API lấy paymentUrl → Trả URL cho Activity
     *
     * @param userId      UID người dùng Firebase Auth
     * @param items       Danh sách sản phẩm trong giỏ hàng
     * @param subtotal    Tổng tiền hàng
     * @param tax         Thuế
     * @param delivery    Phí giao hàng
     * @param totalAmount Tổng cộng thanh toán
     */
    fun placeOrderWithVnPay(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading

            try {
                // Bước 1: Tạo đơn hàng trên Firestore với paymentMethod = "VNPAY", paymentStatus = "unpaid"
                val orderResult = repository.createOrder(
                    userId = userId,
                    items = items,
                    subtotal = subtotal,
                    tax = tax,
                    shippingFee = delivery,
                    totalAmount = totalAmount,
                    paymentMethod = "VNPAY"
                )

                if (orderResult.isFailure) {
                    _checkoutState.value = CheckoutState.Error(
                        orderResult.exceptionOrNull()?.message ?: "Lỗi tạo đơn hàng"
                    )
                    return@launch
                }

                val orderId = orderResult.getOrNull() ?: ""

                // Bước 2: Lấy tỷ giá USD → VND thời gian thực từ API
                // Có cache 1 giờ, fallback về 25,000 nếu API lỗi
                val exchangeRate = ExchangeRateService.getUsdToVndRate()
                val amountInVnd = totalAmount * exchangeRate
                Log.d("CheckoutVM", "Tỷ giá USD→VND: $exchangeRate | $totalAmount USD = $amountInVnd VND")

                // Bước 3: Gọi API Backend để tạo URL thanh toán VNPAY
                val paymentRequest = PaymentRequest(
                    orderId = orderId,
                    amount = amountInVnd
                )

                val response = vnPayApiService.createPaymentUrl(paymentRequest)

                // Bước 3: Trả URL thanh toán về cho Activity để mở WebView
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
        /** Trạng thái ban đầu, chưa có hành động */
        object Idle : CheckoutState()

        /** Đang xử lý (tạo đơn hàng / gọi API) */
        object Loading : CheckoutState()

        /** Đặt hàng COD thành công */
        data class Success(val orderId: String) : CheckoutState()

        /** URL thanh toán VNPAY đã sẵn sàng, Activity cần mở WebView */
        data class PaymentUrlReady(val orderId: String, val paymentUrl: String) : CheckoutState()

        /** Có lỗi xảy ra */
        data class Error(val message: String) : CheckoutState()
    }
}
