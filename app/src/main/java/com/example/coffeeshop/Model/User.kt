package com.example.coffeeshop.Model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
