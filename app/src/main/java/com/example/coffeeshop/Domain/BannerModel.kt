package com.example.coffeeshop.Domain

import com.google.firebase.database.PropertyName

data class BannerModel(
    val url: String = "",
    val title: String = "",
    @get:PropertyName("isHidden") @set:PropertyName("isHidden")
    var isHidden: Boolean = false,
    val createdAt: Long = 0
)