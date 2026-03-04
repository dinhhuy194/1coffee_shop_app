package com.example.coffeeshop.Model

data class Privilege(
    val title: String,
    val description: String,
    val iconEmoji: String,          // Dùng emoji để tránh phụ thuộc drawable
    val requiredRank: String        // RANK_NORMAL / RANK_SILVER / RANK_GOLD / RANK_DIAMOND
) {
    /**
     * Tính alpha hiển thị dựa trên hạng của user.
     * Nếu userRank >= requiredRank thì alpha = 1.0f (sáng rõ),
     * ngược lại alpha = 0.35f (mờ, tạo cảm giác "locked").
     */
    fun getAlphaForUser(userRank: String): Float {
        val rankOrder = listOf(
            User.RANK_NORMAL,
            User.RANK_SILVER,
            User.RANK_GOLD,
            User.RANK_DIAMOND
        )
        val userIndex = rankOrder.indexOf(userRank)
        val requiredIndex = rankOrder.indexOf(requiredRank)
        return if (userIndex >= requiredIndex) 1.0f else 0.35f
    }

    fun isUnlocked(userRank: String): Boolean = getAlphaForUser(userRank) == 1.0f
}
