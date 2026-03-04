package com.example.coffeeshop.Model

data class BeanVoucher(
    val id: String,
    val name: String,
    val description: String,
    val beanCost: Long,         // Số BEAN cần để đổi
    val discountValue: Long,    // Giá trị giảm giá (VNĐ)
    val iconEmoji: String       // Emoji đại diện cho voucher
)
