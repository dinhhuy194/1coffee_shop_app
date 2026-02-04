package com.example.coffeeshop.Model

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val delivery: Double = 0.0,
    val totalAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending" // pending, completed, cancelled
)

data class OrderItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val selectedSize: String = "",
    val iceOption: String = "",
    val sugarOption: String = ""
)
