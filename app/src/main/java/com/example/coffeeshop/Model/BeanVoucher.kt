package com.example.coffeeshop.Model

import com.google.firebase.database.PropertyName

data class BeanVoucher(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val beanCost: Long = 0,         // Số BEAN cần để đổi
    val discountValue: Long = 0,    // Giá trị giảm giá
    val discountType: String = DISCOUNT_TYPE_FIXED, // Loại discount
    val iconEmoji: String = "🎫"    // Emoji đại diện cho voucher
) {
    companion object {
        /** Giảm giá cố định (VD: giảm 50.000đ) */
        const val DISCOUNT_TYPE_FIXED = "fixed"
        /** Giảm theo phần trăm (VD: giảm 10%) */
        const val DISCOUNT_TYPE_PERCENT = "percent"
        /** Miễn phí giao hàng */
        const val DISCOUNT_TYPE_FREE_SHIP = "free_ship"
    }

    /**
     * Tính số tiền được giảm dựa trên subtotal và loại discount.
     * @param subtotal Tổng tiền hàng (VNĐ)
     * @return Số tiền được giảm (VNĐ)
     */
    fun calculateDiscount(subtotal: Double): Double {
        return when (discountType) {
            DISCOUNT_TYPE_FIXED -> discountValue.toDouble()
            DISCOUNT_TYPE_PERCENT -> subtotal * discountValue / 100.0
            DISCOUNT_TYPE_FREE_SHIP -> 0.0 // Xử lý riêng ở checkout (trừ shippingFee)
            else -> discountValue.toDouble()
        }
    }

    /** Chuỗi mô tả giá trị discount để hiển thị UI */
    val discountLabel: String
        get() = when (discountType) {
            DISCOUNT_TYPE_FIXED -> "Giảm ${String.format("%,.0f", discountValue.toDouble())}đ"
            DISCOUNT_TYPE_PERCENT -> "Giảm $discountValue%"
            DISCOUNT_TYPE_FREE_SHIP -> "Miễn phí giao hàng"
            else -> "Giảm ${String.format("%,.0f", discountValue.toDouble())}đ"
        }
}
