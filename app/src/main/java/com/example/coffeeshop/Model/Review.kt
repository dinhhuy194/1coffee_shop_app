package com.example.coffeeshop.Model

data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val userName: String = "",
    val itemId: String = "",     // Dùng item.title (key trên RTDB)
    val rating: Int = 0,         // 1-5 sao
    val comment: String = "",
    val createdAt: Long = 0L,
    val isHidden: Boolean = false,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList()
)
