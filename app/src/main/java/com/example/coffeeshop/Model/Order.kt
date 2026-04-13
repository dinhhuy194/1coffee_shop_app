package com.example.coffeeshop.Model

/**
 * Data class đại diện cho chi tiết thanh toán (lưu bởi VNPAY callback).
 */
data class PaymentDetails(
    val transactionNo: String = "",
    val bankCode: String = "",
    val payDate: String = ""
)

/**
 * Data class đại diện cho đơn hàng.
 * Khớp với OrderManagement.tsx trên admin dashboard + OrderRepository.kt
 *
 * @param orderId        Mã đơn hàng duy nhất
 * @param userId         UID của người đặt
 * @param items          Danh sách sản phẩm
 * @param subtotal       Tổng tiền hàng
 * @param tax            Thuế
 * @param shippingFee    Phí giao hàng
 * @param discountAmount Tiền giảm từ voucher
 * @param totalAmount    Tổng cộng
 * @param voucherId      ID voucher đã dùng
 * @param discountType   Loại discount: "fixed", "percent", "shipping"
 * @param orderStatus    Trạng thái: "pending", "preparing", "ready", "completed", "cancelled"
 * @param paymentMethod  Phương thức thanh toán: "COD", "VNPAY"
 * @param paymentStatus  Trạng thái thanh toán: "unpaid", "paid", "failed"
 * @param paymentDetails Chi tiết thanh toán VNPAY
 * @param timestamp      Thời gian tạo đơn (milliseconds)
 */
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val shippingFee: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val voucherId: String = "",
    val discountType: String = "",
    val orderStatus: String = STATUS_PENDING,
    val paymentMethod: String = "",
    val paymentStatus: String = "",
    val paymentDetails: PaymentDetails = PaymentDetails(),
    val timestamp: Long = System.currentTimeMillis(),
    val address: String = "",
    val userName: String = "",
    val userPhone: String = ""
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_PREPARING = "preparing"
        const val STATUS_READY = "ready"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"

        val ALL_STATUSES = listOf(
            STATUS_PENDING, STATUS_PREPARING, STATUS_READY,
            STATUS_COMPLETED, STATUS_CANCELLED
        )

        fun getStatusLabel(status: String): String = when (status) {
            STATUS_PENDING -> "Chờ xử lý"
            STATUS_PREPARING -> "Đang pha chế"
            STATUS_READY -> "Sẵn sàng giao"
            STATUS_COMPLETED -> "Đã giao"
            STATUS_CANCELLED -> "Đã hủy"
            else -> status
        }
    }
}

/**
 * Một mặt hàng trong đơn hàng.
 */
data class OrderItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val selectedSize: String = "",
    val iceOption: String = "",
    val sugarOption: String = "",
    val imageUrl: String = ""
)
