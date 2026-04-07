package com.example.coffeeshop.Model

/**
 * Data class đại diện cho chi tiết thanh toán (lồng bên trong Order).
 */
data class PaymentDetails(
    val transactionNo: String = "",
    val bankCode: String = "",
    val payDate: String = ""
)

/**
 * Data class đại diện cho một đơn hàng trong hệ thống Coffee Shop.
 *
 * @param orderId        Mã đơn hàng duy nhất (VD: "ORD_1772371164150_5c042a21")
 * @param userId         UID của người dùng Firebase Auth
 * @param items          Danh sách các món trong đơn hàng
 * @param subtotal       Tổng tiền hàng (chưa tính thuế và phí ship)
 * @param tax            Thuế
 * @param shippingFee    Phí giao hàng
 * @param discountAmount Số tiền được giảm từ voucher (0 nếu không dùng)
 * @param totalAmount    Tổng cộng (subtotal + tax + shippingFee - discountAmount)
 * @param voucherId      ID của redeemed_voucher đã dùng (rỗng nếu không dùng)
 * @param discountType   Loại discount: "fixed" / "percent" / "free_ship"
 * @param orderStatus    Trạng thái đơn hàng: "pending", "preparing", "ready", "completed", "cancelled"
 * @param paymentMethod  Phương thức thanh toán: "COD", "VNPAY"
 * @param paymentStatus  Trạng thái thanh toán: "unpaid", "paid", "failed"
 * @param paymentDetails Thông tin giao dịch chi tiết từ VNPAY
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
    val orderStatus: String = "pending",
    val paymentMethod: String = "COD",
    val paymentStatus: String = "unpaid",
    val paymentDetails: PaymentDetails = PaymentDetails(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class đại diện cho một mặt hàng trong đơn hàng.
 */
data class OrderItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val selectedSize: String = "",
    val iceOption: String = "",
    val sugarOption: String = ""
)
