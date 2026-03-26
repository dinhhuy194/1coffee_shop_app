package com.example.coffeeshop.Domain

data class BannerModel(
    val url: String = "",
    val title: String = "",
    val isHidden: Boolean = false,
    val createdAt: Long = 0
)