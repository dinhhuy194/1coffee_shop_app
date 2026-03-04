package com.example.coffeeshop.Model

data class RedeemedVoucher(
    val id: String = "",
    val voucherId: String = "",       // ID gốc của BeanVoucher
    val name: String = "",
    val description: String = "",
    val discountValue: Long = 0,      // Giá trị giảm giá
    val iconEmoji: String = "🎫",
    val redeemedAt: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false,      // Đã sử dụng chưa
    val usedAt: Long = 0              // Thời gian sử dụng
) {
    val statusText: String
        get() = if (isUsed) "Đã sử dụng" else "Chưa dùng"
}
