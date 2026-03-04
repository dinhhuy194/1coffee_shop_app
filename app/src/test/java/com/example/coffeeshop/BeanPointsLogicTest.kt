package com.example.coffeeshop

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test thuần cho logic tích điểm BEAN.
 * Sao chép logic từ User.kt để test độc lập, không phụ thuộc Android/Firebase.
 */
class BeanPointsLogicTest {

    // Sao chép logic từ User.kt companion object
    companion object {
        const val RANK_NORMAL = "Normal"
        const val RANK_SILVER = "Silver"
        const val RANK_GOLD = "Gold"
        const val RANK_DIAMOND = "Diamond"

        fun getRankFromLifetimePoints(lifetimePoints: Long): String = when {
            lifetimePoints >= 1500 -> RANK_DIAMOND
            lifetimePoints >= 500  -> RANK_GOLD
            lifetimePoints >= 100  -> RANK_SILVER
            else                   -> RANK_NORMAL
        }

        fun getBeansPerDollar(rank: String): Long = when (rank) {
            RANK_SILVER  -> 3L
            RANK_GOLD    -> 4L
            RANK_DIAMOND -> 5L
            else         -> 2L
        }
    }

    // ═══ Test getBeansPerDollar ═══════════════════════════════════════════════

    @Test
    fun normalRankGets2BeanPerDollar() {
        assertEquals(2L, getBeansPerDollar(RANK_NORMAL))
    }

    @Test
    fun silverRankGets3BeanPerDollar() {
        assertEquals(3L, getBeansPerDollar(RANK_SILVER))
    }

    @Test
    fun goldRankGets4BeanPerDollar() {
        assertEquals(4L, getBeansPerDollar(RANK_GOLD))
    }

    @Test
    fun diamondRankGets5BeanPerDollar() {
        assertEquals(5L, getBeansPerDollar(RANK_DIAMOND))
    }

    // ═══ Test BEAN earned per order ══════════════════════════════════════════

    @Test
    fun normal5DollarOrderEarns10Bean() {
        assertEquals(10L, 5L * getBeansPerDollar(RANK_NORMAL))
    }

    @Test
    fun silver10DollarOrderEarns30Bean() {
        assertEquals(30L, 10L * getBeansPerDollar(RANK_SILVER))
    }

    // ═══ Test getRankFromLifetimePoints ══════════════════════════════════════

    @Test
    fun zeroPointsIsNormal() {
        assertEquals(RANK_NORMAL, getRankFromLifetimePoints(0))
    }

    @Test
    fun ninetyNinePointsIsNormal() {
        assertEquals(RANK_NORMAL, getRankFromLifetimePoints(99))
    }

    @Test
    fun hundredPointsIsSilver() {
        assertEquals(RANK_SILVER, getRankFromLifetimePoints(100))
    }

    @Test
    fun fiveHundredPointsIsGold() {
        assertEquals(RANK_GOLD, getRankFromLifetimePoints(500))
    }

    @Test
    fun fifteenHundredPointsIsDiamond() {
        assertEquals(RANK_DIAMOND, getRankFromLifetimePoints(1500))
    }

    // ═══ Test rank KHÔNG tụt khi đổi voucher ════════════════════════════════

    @Test
    fun rankStaysAfterRedeemingVoucher() {
        val lifetimePoints = 150L
        val totalPoints = 150L
        val voucherCost = 100L

        assertEquals(RANK_SILVER, getRankFromLifetimePoints(lifetimePoints))

        val newTotalPoints = totalPoints - voucherCost  // 50
        // lifetimePoints giữ nguyên 150 → rank vẫn Silver
        assertEquals(RANK_SILVER, getRankFromLifetimePoints(lifetimePoints))
        assertEquals(50L, newTotalPoints)
    }

    // ═══ Test simulation mua hàng Normal → Silver ═══════════════════════════

    @Test
    fun simulateNormalToSilverRankUp() {
        var totalPoints = 0L
        var lifetimePoints = 0L
        var rank = RANK_NORMAL

        // 10 đơn × $5 ở Normal (2 BEAN/$) = 100 BEAN
        repeat(10) {
            val beans = 5L * getBeansPerDollar(rank)
            totalPoints += beans
            lifetimePoints += beans
            rank = getRankFromLifetimePoints(lifetimePoints)
        }

        assertEquals(100L, lifetimePoints)
        assertEquals(RANK_SILVER, rank)

        // Đơn tiếp theo ở Silver: $5 = 15 BEAN
        val nextBeans = 5L * getBeansPerDollar(rank)
        assertEquals(15L, nextBeans)
    }
}
