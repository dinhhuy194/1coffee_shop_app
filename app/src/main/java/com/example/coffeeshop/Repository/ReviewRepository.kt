package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.Review
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReviewRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()

    /**
     * Ghi review mới lên Firestore, sau đó tính lại rating trung bình
     * và cập nhật rating + reviewCount trên Realtime Database.
     *
     * @param itemId   Key của món trong RTDB node "Items" (thường = item.title)
     * @param userId   UID người dùng hiện tại
     * @param userName Tên hiển thị
     * @param rating   Số sao (1–5)
     * @param comment  Nhận xét văn bản
     */
    suspend fun submitReviewAndUpdateRating(
        itemId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String
    ) {
        // ─── BƯỚC 1: Ghi review lên Firestore ────────────────────────────────
        val reviewRef = firestore.collection("reviews").document()
        val review = Review(
            reviewId  = reviewRef.id,
            userId    = userId,
            userName  = userName,
            itemId    = itemId,
            rating    = rating,
            comment   = comment,
            createdAt = System.currentTimeMillis(),
            isHidden  = false
        )
        reviewRef.set(review).await()

        // ─── BƯỚC 2: Đọc rating + reviewCount hiện tại từ RTDB ───────────────
        val itemRef = rtdb.getReference("Items").child(itemId)
        val snapshot = itemRef.getSuspend()

        val oldRating      = snapshot.child("rating").getValue(Double::class.java) ?: 0.0
        val oldReviewCount = snapshot.child("reviewCount").getValue(Long::class.java) ?: 0L

        // ─── BƯỚC 3: Tính rating mới theo công thức trung bình cộng ──────────
        val newReviewCount = oldReviewCount + 1
        val newAvgRating   = ((oldRating * oldReviewCount) + rating) / newReviewCount

        // ─── BƯỚC 4: Cập nhật lại RTDB ───────────────────────────────────────
        val updates = mapOf<String, Any>(
            "rating"      to newAvgRating,
            "reviewCount" to newReviewCount
        )
        itemRef.updateChildren(updates).await()
    }

    /**
     * Kiểm tra user đã từng review món này chưa để tránh duplicate.
     */
    suspend fun hasUserReviewed(itemId: String, userId: String): Boolean {
        val result = firestore.collection("reviews")
            .whereEqualTo("itemId", itemId)
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()
        return !result.isEmpty
    }
}

// ─── Extension: ValueEventListener → suspend ─────────────────────────────────
private suspend fun com.google.firebase.database.DatabaseReference.getSuspend(): DataSnapshot =
    suspendCancellableCoroutine { cont ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cont.resume(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(error.toException())
            }
        }
        addListenerForSingleValueEvent(listener)
        cont.invokeOnCancellation { removeEventListener(listener) }
    }
