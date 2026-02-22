package com.example.coffeeshop.Model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalPoints: Long = 0,
    val rank: String = "Normal",
    val totalSpent: Long = 0
) {
    companion object {
        const val RANK_NORMAL = "Normal"
        const val RANK_SILVER = "Silver"
        const val RANK_GOLD = "Gold"
        const val RANK_DIAMOND = "Diamond"

        fun getRankFromPoints(points: Long): String = when {
            points >= 5000 -> RANK_DIAMOND
            points >= 2000 -> RANK_GOLD
            points >= 500  -> RANK_SILVER
            else           -> RANK_NORMAL
        }

        fun getNextRankThreshold(rank: String): Long = when (rank) {
            RANK_NORMAL  -> 500L
            RANK_SILVER  -> 2000L
            RANK_GOLD    -> 5000L
            else         -> -1L // Diamond — không có hạng tiếp theo
        }

        fun getCurrentRankMinPoints(rank: String): Long = when (rank) {
            RANK_SILVER  -> 500L
            RANK_GOLD    -> 2000L
            RANK_DIAMOND -> 5000L
            else         -> 0L
        }

        fun getBeansPerUnit(rank: String): Long = when (rank) {
            RANK_SILVER  -> 7L
            RANK_GOLD    -> 8L
            RANK_DIAMOND -> 9L
            else         -> 6L // Normal
        }
    }
}
