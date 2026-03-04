package com.example.coffeeshop.Model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalPoints: Long = 0,
    val rank: String = "Normal",
    val totalSpent: Long = 0,
    val lifetimePoints: Long = 0
) {
    companion object {
        const val RANK_NORMAL = "Normal"
        const val RANK_SILVER = "Silver"
        const val RANK_GOLD = "Gold"
        const val RANK_DIAMOND = "Diamond"

        /**
         * Xác định hạng thành viên dựa trên lifetimePoints
         * (tổng BEAN đã từng tích lũy, không bị trừ khi đổi quà).
         */
        fun getRankFromLifetimePoints(lifetimePoints: Long): String = when {
            lifetimePoints >= 1500 -> RANK_DIAMOND
            lifetimePoints >= 500  -> RANK_GOLD
            lifetimePoints >= 100  -> RANK_SILVER
            else                   -> RANK_NORMAL
        }

        /**
         * Ngưỡng lifetimePoints cần đạt để lên hạng tiếp theo.
         */
        fun getNextRankThreshold(rank: String): Long = when (rank) {
            RANK_NORMAL  -> 100L
            RANK_SILVER  -> 500L
            RANK_GOLD    -> 1500L
            else         -> -1L // Diamond — hạng cao nhất
        }

        /**
         * Điểm lifetimePoints tối thiểu của hạng hiện tại.
         */
        fun getCurrentRankMinPoints(rank: String): Long = when (rank) {
            RANK_SILVER  -> 100L
            RANK_GOLD    -> 500L
            RANK_DIAMOND -> 1500L
            else         -> 0L
        }

        /**
         * Số BEAN nhận được trên mỗi $1 chi tiêu.
         * Hạng cao hơn → tích nhiều BEAN hơn.
         */
        fun getBeansPerDollar(rank: String): Long = when (rank) {
            RANK_SILVER  -> 3L
            RANK_GOLD    -> 4L
            RANK_DIAMOND -> 5L
            else         -> 2L  // Normal: 1$ = 2 BEAN
        }
    }
}
