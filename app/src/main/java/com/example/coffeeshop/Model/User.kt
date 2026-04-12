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
    val lifetimePoints: Long = 0,
    // Địa chỉ giao hàng - được cập nhật từ MapboxPickerActivity
    val address: String? = null,
    // RBAC role — đồng bộ với AuthContext.tsx trên admin dashboard
    val role: String = ROLE_CUSTOMER
) {
    /** Kiểm tra user có quyền admin (admin hoặc superadmin) */
    fun isAdmin(): Boolean = role == ROLE_ADMIN || role == ROLE_SUPERADMIN

    /** Kiểm tra user là superadmin */
    fun isSuperAdmin(): Boolean = role == ROLE_SUPERADMIN

    /** Kiểm tra user có quyền staff trở lên */
    fun isStaffOrAbove(): Boolean = role == ROLE_STAFF || role == ROLE_ADMIN || role == ROLE_SUPERADMIN

    companion object {
        // ── Role constants — khớp Firestore field "role" ──
        const val ROLE_CUSTOMER = "customer"
        const val ROLE_STAFF = "staff"
        const val ROLE_ADMIN = "admin"
        const val ROLE_SUPERADMIN = "superadmin"

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
