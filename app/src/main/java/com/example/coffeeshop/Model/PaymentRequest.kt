package com.example.coffeeshop.Model

import com.google.gson.annotations.SerializedName

/**
 * Model gửi lên API Backend để yêu cầu tạo URL thanh toán VNPAY.
 *
 * @param orderId Mã đơn hàng (VD: "ORD_1772371164150_5c042a21")
 * @param amount  Tổng số tiền thanh toán (đã nhân tỷ giá VND nếu cần)
 */
data class PaymentRequest(
    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("amount")
    val amount: Double
)

/**
 * Model nhận response từ API Backend sau khi tạo URL thanh toán.
 *
 * @param paymentUrl URL thanh toán VNPAY (mở trong WebView hoặc trình duyệt)
 */
data class PaymentUrlResponse(
    @SerializedName("paymentUrl")
    val paymentUrl: String
)
