package com.example.coffeeshop.Model

/**
 * Data class đại diện cho chi tiết thanh toán (lồng bên trong Order).
 * Chứa thông tin giao dịch từ cổng thanh toán VNPAY.
 *
 * @param transactionNo Mã giao dịch từ VNPAY (trả về sau khi thanh toán)
 * @param bankCode      Mã ngân hàng người dùng đã chọn để thanh toán
 * @param payDate        Ngày giờ thanh toán (định dạng: yyyyMMddHHmmss)
 */
data class PaymentDetails(
    val transactionNo: String = "",
    val bankCode: String = "",
    val payDate: String = ""
)

/**
 * Data class đại diện cho một đơn hàng trong hệ thống Coffee Shop.
 * Cấu trúc này khớp với JSON document trong Firestore collection "orders".
 *
 * @param orderId        Mã đơn hàng duy nhất (VD: "ORD_1772371164150_5c042a21")
 * @param userId         UID của người dùng Firebase Auth
 * @param items          Danh sách các món trong đơn hàng
 * @param subtotal       Tổng tiền hàng (chưa tính thuế và phí ship)
 * @param tax            Thuế
 * @param shippingFee    Phí giao hàng
 * @param totalAmount    Tổng cộng (subtotal + tax + shippingFee)
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
    val totalAmount: Double = 0.0,
    val orderStatus: String = "pending",
    val paymentMethod: String = "COD",
    val paymentStatus: String = "unpaid",
    val paymentDetails: PaymentDetails = PaymentDetails(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class đại diện cho một mặt hàng trong đơn hàng.
 *
 * @param title        Tên sản phẩm
 * @param price        Giá của sản phẩm
 * @param quantity     Số lượng
 * @param selectedSize Kích thước đã chọn (Small, Medium, Large)
 * @param iceOption    Tùy chọn đá (Đá chung, Đá riêng, Không đá)
 * @param sugarOption  Tùy chọn đường (Bình thường, Ít đường, Không đường)
 */
data class OrderItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val selectedSize: String = "",
    val iceOption: String = "",
    val sugarOption: String = ""
)
