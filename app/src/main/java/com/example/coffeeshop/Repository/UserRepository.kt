package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.BeanVoucher
import com.example.coffeeshop.Model.PointHistory
import com.example.coffeeshop.Model.RedeemedVoucher
import com.example.coffeeshop.Model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Lấy thông tin user từ Firestore
     */
    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            var user = document.toObject(User::class.java)
            if (user != null) {
                // Auto-migration: nếu user cũ chưa có lifetimePoints
                val migratedUser = migrateLifetimePointsIfNeeded(user)
                Result.success(migratedUser)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migration tự động cho user cũ: nếu lifetimePoints == 0 nhưng totalPoints > 0,
     * nghĩa là user đã tích điểm trước khi có lifetimePoints.
     * Set lifetimePoints = totalPoints và cập nhật rank dựa trên lifetimePoints.
     * Chỉ chạy 1 lần — sau khi migration, lifetimePoints > 0 nên sẽ không chạy lại.
     */
    private suspend fun migrateLifetimePointsIfNeeded(user: User): User {
        if (user.lifetimePoints == 0L && user.totalPoints > 0L) {
            val newLifetime = user.totalPoints
            val newRank = User.getRankFromLifetimePoints(newLifetime)
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .update(
                        mapOf(
                            "lifetimePoints" to newLifetime,
                            "rank" to newRank
                        )
                    ).await()
                return user.copy(lifetimePoints = newLifetime, rank = newRank)
            } catch (_: Exception) {
                // Nếu migration thất bại, trả về user gốc — sẽ thử lại lần sau
                return user
            }
        }
        return user
    }

    /**
     * Cập nhật thông tin user
     */
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xử lý tích điểm BEAN sau khi đặt hàng thành công.
     * - totalPoints: số dư BEAN khả dụng (dùng để đổi quà)
     * - lifetimePoints: tổng BEAN đã tích lũy (dùng để xác định rank, không bị trừ)
     * - rank: dựa trên lifetimePoints
     */
    suspend fun processPointsEarned(
        userId: String,
        orderAmount: Long,
        orderId: String = ""
    ): Result<Long> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val historyRef = userRef.collection("point_history").document()

            var beansEarned = 0L

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("totalPoints") ?: 0L
                val currentRank = snapshot.getString("rank") ?: User.RANK_NORMAL
                val currentSpent = snapshot.getLong("totalSpent") ?: 0L
                val currentLifetime = snapshot.getLong("lifetimePoints") ?: 0L

                // Tính BEAN dựa trên rank hiện tại: 1$ = 2/3/4/5 BEAN
                beansEarned = orderAmount * User.getBeansPerDollar(currentRank)

                val newTotalPoints = currentPoints + beansEarned
                val newLifetimePoints = currentLifetime + beansEarned
                // Rank dựa trên lifetimePoints (tổng tích lũy), KHÔNG dựa trên số dư
                val newRank = User.getRankFromLifetimePoints(newLifetimePoints)

                val updateMap = mapOf(
                    "totalPoints"    to newTotalPoints,
                    "lifetimePoints" to newLifetimePoints,
                    "rank"           to newRank,
                    "totalSpent"     to (currentSpent + orderAmount)
                )
                transaction.update(userRef, updateMap)

                if (beansEarned > 0) {
                    val historyData = mapOf(
                        "id"        to historyRef.id,
                        "delta"     to beansEarned,
                        "reason"    to if (orderId.isNotEmpty()) "Order #$orderId" else "Đặt hàng",
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(historyRef, historyData)
                }
            }.await()

            Result.success(beansEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đổi voucher bằng BEAN + lưu voucher đã đổi vào subcollection.
     * CHỈ trừ totalPoints (số dư), KHÔNG ảnh hưởng lifetimePoints và rank.
     */
    suspend fun redeemVoucher(
        userId: String,
        voucherBeanCost: Long,
        voucherName: String,
        voucher: BeanVoucher? = null
    ): Result<Long> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val historyRef = userRef.collection("point_history").document()
            val redeemedRef = userRef.collection("redeemed_vouchers").document()

            var remainingPoints = 0L

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("totalPoints") ?: 0L

                if (currentPoints < voucherBeanCost) {
                    throw Exception("Không đủ BEAN để đổi voucher")
                }

                remainingPoints = currentPoints - voucherBeanCost

                // Chỉ cập nhật totalPoints, KHÔNG thay đổi rank hay lifetimePoints
                transaction.update(userRef, "totalPoints", remainingPoints)

                // Ghi lịch sử điểm âm
                val historyData = mapOf(
                    "id"        to historyRef.id,
                    "delta"     to -voucherBeanCost,
                    "reason"    to "Đổi ${voucherName}",
                    "timestamp" to System.currentTimeMillis()
                )
                transaction.set(historyRef, historyData)

                // Lưu voucher đã đổi vào subcollection redeemed_vouchers
                if (voucher != null) {
                    val redeemedData = mapOf(
                        "id"            to redeemedRef.id,
                        "voucherId"     to voucher.id,
                        "name"          to voucher.name,
                        "description"   to voucher.description,
                        "discountValue" to voucher.discountValue,
                        "iconEmoji"     to voucher.iconEmoji,
                        "redeemedAt"    to System.currentTimeMillis(),
                        "isUsed"        to false,
                        "usedAt"        to 0L
                    )
                    transaction.set(redeemedRef, redeemedData)
                }
            }.await()

            Result.success(remainingPoints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách voucher đã đổi của user
     */
    suspend fun getRedeemedVouchers(userId: String): Result<List<RedeemedVoucher>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("redeemed_vouchers")
                .orderBy("redeemedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val vouchers = snapshot.documents.mapNotNull { it.toObject(RedeemedVoucher::class.java) }
            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đánh dấu voucher đã sử dụng
     */
    suspend fun markVoucherUsed(userId: String, voucherId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("redeemed_vouchers")
                .document(voucherId)
                .update(
                    mapOf(
                        "isUsed" to true,
                        "usedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy 20 giao dịch gần nhất từ sub-collection point_history
     */
    suspend fun getPointHistory(userId: String): Result<List<PointHistory>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("point_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            val history = snapshot.documents.mapNotNull { it.toObject(PointHistory::class.java) }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
