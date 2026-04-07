package com.example.coffeeshop.Model

data class RedeemedVoucher(
    val id: String = "",
    val voucherId: String = "",           // ID gốc của BeanVoucher
    val name: String = "",
    val description: String = "",
    val discountValue: Long = 0,          // Giá trị giảm giá
    val discountType: String = BeanVoucher.DISCOUNT_TYPE_FIXED, // Loại discount
    val iconEmoji: String = "🎫",
    val redeemedAt: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false,          // Đã sử dụng chưa
    val usedAt: Long = 0                  // Thời gian sử dụng
) {
    val statusText: String
        get() = if (isUsed) "Đã sử dụng" else "Chưa dùng"

    /** Chuỗi mô tả giá trị discount để hiển thị UI */
    val discountLabel: String
        get() = when (discountType) {
            BeanVoucher.DISCOUNT_TYPE_FIXED -> "Giảm ${String.format("%,.0f", discountValue.toDouble())}đ"
            BeanVoucher.DISCOUNT_TYPE_PERCENT -> "Giảm $discountValue%"
            BeanVoucher.DISCOUNT_TYPE_FREE_SHIP -> "Miễn phí giao hàng"
            else -> "Giảm ${String.format("%,.0f", discountValue.toDouble())}đ"
        }

    /**
     * Tính số tiền được giảm dựa trên subtotal và loại discount.
     */
    fun calculateDiscount(subtotal: Double): Double {
        return when (discountType) {
            BeanVoucher.DISCOUNT_TYPE_FIXED -> discountValue.toDouble()
            BeanVoucher.DISCOUNT_TYPE_PERCENT -> subtotal * discountValue / 100.0
            BeanVoucher.DISCOUNT_TYPE_FREE_SHIP -> 0.0
            else -> discountValue.toDouble()
        }
    }
}
