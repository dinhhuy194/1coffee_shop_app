package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Model.OrderItem
import com.example.coffeeshop.Model.PaymentDetails
import com.example.coffeeshop.Domain.ItemsModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository xử lý các thao tác với collection "orders" trên Firestore.
 * Bao gồm: tạo đơn hàng, lấy lịch sử đơn hàng.
 */
class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Tạo đơn hàng mới trên Firestore.
     *
     * @param userId        UID người dùng Firebase Auth
     * @param items         Danh sách sản phẩm từ giỏ hàng
     * @param subtotal      Tổng tiền hàng
     * @param tax           Thuế
     * @param shippingFee   Phí giao hàng
     * @param totalAmount   Tổng cộng
     * @param paymentMethod Phương thức thanh toán: "COD" hoặc "VNPAY"
     * @return Result chứa orderId nếu thành công, hoặc Exception nếu thất bại
     */
    suspend fun createOrder(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        shippingFee: Double,
        totalAmount: Double,
        paymentMethod: String = "COD",
        voucherId: String = "",
        discountAmount: Double = 0.0,
        discountType: String = ""
    ): Result<String> {
        return try {
            // Tạo mã đơn hàng duy nhất: ORD_<timestamp>_<uuid_8_ký_tự>
            val orderId = "ORD_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            
            // Chuyển đổi ItemsModel (Domain) sang OrderItem (Model)
            val orderItems = items.map { item ->
                OrderItem(
                    title = item.title,
                    price = item.price,
                    quantity = item.numberInCart,
                    selectedSize = item.selectedSize,
                    iceOption = item.iceOption,
                    sugarOption = item.sugarOption,
                    imageUrl = if (item.picUrl.isNotEmpty()) item.picUrl[0] else ""
                )
            }

            val initialPaymentStatus = "unpaid"
            
            // Tạo object Order với đầy đủ thông tin, bao gồm voucher
            val order = Order(
                orderId = orderId,
                userId = userId,
                items = orderItems,
                subtotal = subtotal,
                tax = tax,
                shippingFee = shippingFee,
                discountAmount = discountAmount,
                totalAmount = totalAmount,
                voucherId = voucherId,
                discountType = discountType,
                orderStatus = "pending",
                paymentMethod = paymentMethod,
                paymentStatus = initialPaymentStatus,
                paymentDetails = PaymentDetails(),
                timestamp = System.currentTimeMillis()
            )
            
            // Lưu đơn hàng vào Firestore collection "orders"
            firestore.collection("orders")
                .document(orderId)
                .set(order)
                .await()
            
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Lấy lịch sử đơn hàng của một người dùng.
     * Kết quả được sắp xếp theo thời gian giảm dần (mới nhất trước).
     *
     * @param userId UID người dùng Firebase Auth
     * @return Result chứa danh sách Order hoặc Exception nếu thất bại
     */
    suspend fun getOrderHistory(userId: String): Result<List<Order>> {
        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Chuyển đổi snapshot sang danh sách Order và sắp xếp giảm dần theo timestamp
            val orders = snapshot.toObjects(Order::class.java)
                .sortedByDescending { it.timestamp }
            
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    /**
     * Cập nhật trạng thái thanh toán cho đơn hàng trên Firestore.
     * Được gọi từ phía client sau khi WebView nhận kết quả từ VNPAY.
     *
     * Hàm này bổ trợ cho IPN callback ở backend:
     * - Trên production: IPN backend sẽ cập nhật Firestore
     * - Trên dev (localhost): VNPAY không gọi được IPN → cần app tự cập nhật
     *
     * @param orderId       Mã đơn hàng (vnp_TxnRef)
     * @param paymentStatus Trạng thái: "paid" hoặc "failed"
     * @param transactionNo Mã giao dịch VNPAY
     * @param bankCode      Mã ngân hàng
     * @param payDate        Ngày thanh toán
     * @return Result<Unit> thành công hoặc Exception
     */
    suspend fun updatePaymentStatus(
        orderId: String,
        paymentStatus: String,
        transactionNo: String = "",
        bankCode: String = "",
        payDate: String = ""
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "paymentStatus" to paymentStatus,
                "paymentDetails" to hashMapOf(
                    "transactionNo" to transactionNo,
                    "bankCode" to bankCode,
                    "payDate" to payDate
                )
            )

            firestore.collection("orders")
                .document(orderId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng (orderStatus) trên Firestore.
     * Ví dụ: "pending" → "completed" sau khi thanh toán thành công.
     *
     * @param orderId     Mã đơn hàng
     * @param orderStatus Trạng thái mới: "pending", "preparing", "completed", "cancelled"
     * @return Result<Unit>
     */
    suspend fun updateOrderStatus(
        orderId: String,
        orderStatus: String
    ): Result<Unit> {
        return try {
            firestore.collection("orders")
                .document(orderId)
                .update("orderStatus", orderStatus)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
