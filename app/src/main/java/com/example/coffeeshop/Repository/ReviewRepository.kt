package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.Review
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReviewRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()

    // ───────────────────────────────────────────────────────────────────────────
    //  SUBMIT
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Ghi review mới lên Firestore, sau đó tính lại rating trung bình
     * và cập nhật rating + reviewCount trên Realtime Database.
     */
    suspend fun submitReviewAndUpdateRating(
        itemId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String
    ) {
        // BƯỚC 1: Ghi review lên Firestore
        val reviewRef = firestore.collection("reviews").document()
        val review = Review(
            reviewId  = reviewRef.id,
            userId    = userId,
            userName  = userName,
            itemId    = itemId,
            rating    = rating,
            comment   = comment,
            createdAt = System.currentTimeMillis(),
            isHidden  = false,
            likes     = 0,
            likedBy   = emptyList()
        )
        reviewRef.set(review).await()

        // BƯỚC 2: Đọc rating + reviewCount hiện tại từ RTDB
        val itemRef = rtdb.getReference("Items").child(itemId)
        val snapshot = itemRef.getSuspend()

        val oldRating      = snapshot.child("rating").getValue(Double::class.java) ?: 0.0
        val oldReviewCount = snapshot.child("reviewCount").getValue(Long::class.java) ?: 0L

        // BƯỚC 3: Tính rating mới theo công thức trung bình cộng
        val newReviewCount = oldReviewCount + 1
        val newAvgRating   = ((oldRating * oldReviewCount) + rating) / newReviewCount

        // BƯỚC 4: Cập nhật lại RTDB
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

    // ───────────────────────────────────────────────────────────────────────────
    //  QUERY
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Load tất cả reviews của 1 sản phẩm, sắp xếp theo likes giảm dần.
     * Chỉ dùng 1 whereEqualTo để tránh yêu cầu composite index.
     * Filter isHidden + sort được thực hiện client-side.
     */
    suspend fun getReviewsForItem(itemId: String): List<Review> {
        return try {
            android.util.Log.d("ReviewRepo", "Querying reviews for itemId='$itemId'")
            val result = firestore.collection("reviews")
                .whereEqualTo("itemId", itemId)
                .get()
                .await()
            android.util.Log.d("ReviewRepo", "Found ${result.size()} raw docs for '$itemId'")
            val reviews = result.toObjects(Review::class.java)
                .filter { !it.isHidden }
                .sortedByDescending { it.likes }
            android.util.Log.d("ReviewRepo", "After filter: ${reviews.size} visible reviews")
            reviews
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepo", "Error loading reviews for '$itemId': ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Load tất cả reviews của user hiện tại, mới nhất trước.
     * Không dùng orderBy để tránh cần composite index, sort client-side.
     */
    suspend fun getReviewsByUser(userId: String): List<Review> {
        return try {
            android.util.Log.d("ReviewRepo", "Querying reviews for userId='$userId'")
            val result = firestore.collection("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            android.util.Log.d("ReviewRepo", "Found ${result.size()} user reviews")
            result.toObjects(Review::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepo", "Error loading user reviews: ${e.message}", e)
            emptyList()
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    //  LIKE / UNLIKE
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Toggle like: nếu user chưa like → thêm, đã like → bỏ.
     * Sử dụng FieldValue.arrayUnion/arrayRemove + increment để atomic update.
     *
     * @return true nếu sau toggle user đã like, false nếu đã unlike
     */
    suspend fun toggleLikeReview(reviewId: String, userId: String): Boolean {
        val docRef = firestore.collection("reviews").document(reviewId)
        val doc = docRef.get().await()
        val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<String>()
        val alreadyLiked = likedBy.contains(userId)

        if (alreadyLiked) {
            // Unlike
            docRef.update(
                "likedBy", FieldValue.arrayRemove(userId),
                "likes", FieldValue.increment(-1)
            ).await()
            return false
        } else {
            // Like
            docRef.update(
                "likedBy", FieldValue.arrayUnion(userId),
                "likes", FieldValue.increment(1)
            ).await()
            return true
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Xóa review và cập nhật lại rating trung bình trên RTDB.
     */
    suspend fun deleteReview(review: Review) {
        // BƯỚC 1: Xóa review khỏi Firestore
        firestore.collection("reviews").document(review.reviewId).delete().await()

        // BƯỚC 2: Đọc rating + reviewCount hiện tại từ RTDB
        val itemRef = rtdb.getReference("Items").child(review.itemId)
        val snapshot = itemRef.getSuspend()

        val oldRating      = snapshot.child("rating").getValue(Double::class.java) ?: 0.0
        val oldReviewCount = snapshot.child("reviewCount").getValue(Long::class.java) ?: 0L

        // BƯỚC 3: Tính lại rating
        val newReviewCount = (oldReviewCount - 1).coerceAtLeast(0)
        val newAvgRating = if (newReviewCount > 0) {
            ((oldRating * oldReviewCount) - review.rating) / newReviewCount
        } else {
            0.0
        }

        // BƯỚC 4: Cập nhật lại RTDB
        val updates = mapOf<String, Any>(
            "rating"      to newAvgRating.coerceAtLeast(0.0),
            "reviewCount" to newReviewCount
        )
        itemRef.updateChildren(updates).await()
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
