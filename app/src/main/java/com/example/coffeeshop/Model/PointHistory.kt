package com.example.coffeeshop.Model

data class PointHistory(
    val id: String = "",
    val delta: Long = 0,                        // +50 hoặc -200 (âm khi đổi voucher)
    val reason: String = "",                    // "Order #ABC" hoặc "Đổi Voucher Cafe Sữa"
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPositive: Boolean get() = delta >= 0
    val formattedDelta: String get() = if (isPositive) "+$delta BEAN" else "$delta BEAN"
}
