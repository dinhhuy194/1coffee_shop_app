package com.example.coffeeshop.Repository

import android.util.Log
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Model.Order
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * AdminRepository — CRUD operations for admin management.
 * Tương ứng với firebaseService.ts trên React admin dashboard.
 *
 * Data sources:
 *   - Category, Items, Banner → Firebase Realtime Database
 *   - Orders → Firestore
 */
class AdminRepository {
    private val rtdb = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ─── CATEGORY CRUD ───────────────────────────────────────────

    suspend fun getAllCategories(): List<CategoryModel> {
        return try {
            val snapshot = rtdb.getReference("Category").get().await()
            val list = mutableListOf<CategoryModel>()
            for (child in snapshot.children) {
                child.getValue(CategoryModel::class.java)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "getAllCategories error: ${e.message}")
            emptyList()
        }
    }

    suspend fun addCategory(title: String): Result<Unit> {
        return try {
            val all = getAllCategories()
            val maxId = all.maxOfOrNull { it.id } ?: -1
            val newId = maxId + 1
            val newList = all.map { mapOf("id" to it.id, "title" to it.title) } +
                    mapOf("id" to newId, "title" to title)
            rtdb.getReference("Category").setValue(newList).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addCategory error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateCategory(id: Int, title: String): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Category").get().await()
            val list = mutableListOf<Map<String, Any?>>()
            for (child in snapshot.children) {
                val cat = child.getValue(CategoryModel::class.java) ?: continue
                if (cat.id == id) {
                    list.add(mapOf("id" to cat.id, "title" to title,
                        "isHidden" to (child.child("isHidden").getValue(Boolean::class.java) ?: false)))
                } else {
                    list.add(mapOf("id" to cat.id, "title" to cat.title,
                        "isHidden" to (child.child("isHidden").getValue(Boolean::class.java) ?: false)))
                }
            }
            rtdb.getReference("Category").setValue(list).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateCategory error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun softDeleteCategory(id: Int): Result<Unit> {
        return setCategoryHidden(id, true)
    }

    suspend fun restoreCategory(id: Int): Result<Unit> {
        return setCategoryHidden(id, false)
    }

    private suspend fun setCategoryHidden(id: Int, hidden: Boolean): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Category").get().await()
            val list = mutableListOf<Map<String, Any?>>()
            for (child in snapshot.children) {
                val cat = child.getValue(CategoryModel::class.java) ?: continue
                val isHidden = if (cat.id == id) hidden
                else (child.child("isHidden").getValue(Boolean::class.java) ?: false)
                list.add(mapOf("id" to cat.id, "title" to cat.title, "isHidden" to isHidden))
            }
            rtdb.getReference("Category").setValue(list).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "setCategoryHidden error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── ITEMS CRUD ──────────────────────────────────────────────

    suspend fun getAllItems(): List<ItemsModel> {
        return try {
            val snapshot = rtdb.getReference("Items").get().await()
            val list = mutableListOf<ItemsModel>()
            for (child in snapshot.children) {
                child.getValue(ItemsModel::class.java)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "getAllItems error: ${e.message}")
            emptyList()
        }
    }

    suspend fun addItem(item: ItemsModel): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Items").get().await()
            val rawData = snapshot.value

            val payload = mapOf(
                "title" to item.title,
                "price" to item.price,
                "rating" to item.rating,
                "description" to item.description,
                "extra" to item.extra,
                "categoryId" to item.categoryId,
                "picUrl" to item.picUrl,
                "isHidden" to false,
                "createdAt" to System.currentTimeMillis()
            )

            if (rawData == null) {
                rtdb.getReference("Items").setValue(listOf(payload)).await()
            } else if (rawData is List<*>) {
                val cleanList = rawData.filterNotNull().toMutableList()
                cleanList.add(payload)
                rtdb.getReference("Items").setValue(cleanList).await()
            } else {
                // Object-based → push
                rtdb.getReference("Items").push().setValue(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addItem error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateItem(itemKey: String, item: ItemsModel): Result<Unit> {
        return try {
            val itemRef = rtdb.getReference("Items").child(itemKey)
            val currentSnapshot = itemRef.get().await()
            val currentHidden = currentSnapshot.child("isHidden").getValue(Boolean::class.java) ?: false

            val payload = mapOf(
                "title" to item.title,
                "price" to item.price,
                "rating" to item.rating,
                "description" to item.description,
                "extra" to item.extra,
                "categoryId" to item.categoryId,
                "picUrl" to item.picUrl,
                "isHidden" to currentHidden
            )

            itemRef.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateItem error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun softDeleteItem(itemKey: String): Result<Unit> = setItemHidden(itemKey, true)
    suspend fun restoreItem(itemKey: String): Result<Unit> = setItemHidden(itemKey, false)

    private suspend fun setItemHidden(itemKey: String, hidden: Boolean): Result<Unit> {
        return try {
            val itemsRef = rtdb.getReference("Items")
            val snapshot = itemsRef.get().await()
            val rawData = snapshot.value
            var updated = false

            Log.d(TAG, "setItemHidden: key=$itemKey, hidden=$hidden, dataType=${rawData?.javaClass?.simpleName}")

            // Nếu dữ liệu là List (array), cập nhật toàn bộ list để tránh lỗi partial update
            val index = itemKey.toIntOrNull()
            if (index != null && rawData is List<*>) {
                val list = rawData.toMutableList()
                if (index in list.indices) {
                    val current = list[index]
                    if (current is Map<*, *>) {
                        val mutable = current.toMutableMap()
                        mutable["isHidden"] = hidden
                        list[index] = mutable
                        itemsRef.setValue(list).await()
                        updated = true
                        Log.d(TAG, "setItemHidden: Updated via list setValue at index=$index")
                    }
                }
            }

            // Fallback: dữ liệu dạng object (push keys)
            if (!updated) {
                for (child in snapshot.children) {
                    if (child.key == itemKey) {
                        child.ref.child("isHidden").setValue(hidden).await()
                        updated = true
                        Log.d(TAG, "setItemHidden: Updated via child ref, key=${child.key}")
                        break
                    }
                }
            }

            if (!updated) {
                Log.e(TAG, "setItemHidden: NOT FOUND key=$itemKey")
                return Result.failure(Exception("Không tìm thấy sản phẩm để cập nhật (key=$itemKey)"))
            }

            syncPopularHiddenFromItem(itemKey, hidden)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "setItemHidden error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun syncPopularHiddenFromItem(itemKey: String, hidden: Boolean) {
        try {
            val itemSnapshot = rtdb.getReference("Items").child(itemKey).get().await()
            if (!itemSnapshot.exists()) return

            val title = itemSnapshot.child("title").getValue(String::class.java) ?: return
            val categoryId = itemSnapshot.child("categoryId").value?.toString() ?: ""
            val firstPic = itemSnapshot.child("picUrl").children.firstOrNull()?.getValue(String::class.java)

            val popularRef = rtdb.getReference("Popular")
            val popularSnapshot = popularRef.get().await()
            val rawPopular = popularSnapshot.value

            // Nếu Popular là array, phải update toàn bộ list
            if (rawPopular is List<*>) {
                val list = rawPopular.toMutableList()
                var changed = false
                for (i in list.indices) {
                    val item = list[i]
                    if (item is Map<*, *>) {
                        val pTitle = item["title"]?.toString() ?: ""
                        val pCategoryId = item["categoryId"]?.toString() ?: ""
                        val pPicUrls = item["picUrl"]
                        val pFirstPic = when (pPicUrls) {
                            is List<*> -> pPicUrls.firstOrNull()?.toString()
                            else -> null
                        }

                        val sameIdentity = pTitle == title &&
                            pCategoryId == categoryId &&
                            (firstPic == null || firstPic == pFirstPic)

                        if (sameIdentity) {
                            val mutable = item.toMutableMap()
                            mutable["isHidden"] = hidden
                            list[i] = mutable
                            changed = true
                        }
                    }
                }
                if (changed) {
                    popularRef.setValue(list).await()
                }
            } else {
                // Dữ liệu dạng object (push keys)
                for (child in popularSnapshot.children) {
                    val pTitle = child.child("title").getValue(String::class.java) ?: ""
                    val pCategoryId = child.child("categoryId").value?.toString() ?: ""
                    val pFirstPic = child.child("picUrl").children.firstOrNull()?.getValue(String::class.java)

                    val sameIdentity = pTitle == title &&
                        pCategoryId == categoryId &&
                        (firstPic == null || firstPic == pFirstPic)

                    if (sameIdentity) {
                        child.ref.child("isHidden").setValue(hidden).await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncPopularHiddenFromItem warning: ${e.message}")
        }
    }

    // ─── BANNER CRUD ─────────────────────────────────────────────

    suspend fun getAllBanners(): List<BannerModel> {
        return try {
            val snapshot = rtdb.getReference("Banner").get().await()
            val list = mutableListOf<BannerModel>()
            for (child in snapshot.children) {
                child.getValue(BannerModel::class.java)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "getAllBanners error: ${e.message}")
            emptyList()
        }
    }

    suspend fun addBanner(url: String, title: String): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Banner").get().await()
            val rawData = snapshot.value

            val payload = mapOf(
                "url" to url,
                "title" to title,
                "isHidden" to false,
                "createdAt" to System.currentTimeMillis()
            )

            if (rawData == null) {
                rtdb.getReference("Banner").setValue(listOf(payload)).await()
            } else if (rawData is List<*>) {
                val cleanList = rawData.filterNotNull().toMutableList()
                cleanList.add(payload)
                rtdb.getReference("Banner").setValue(cleanList).await()
            } else {
                rtdb.getReference("Banner").push().setValue(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addBanner error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateBanner(index: Int, url: String, title: String): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Banner").get().await()
            val rawData = snapshot.value

            if (rawData is List<*>) {
                val list = rawData.toMutableList()
                if (index < 0 || index >= list.size) return Result.failure(Exception("Index out of bounds"))
                val current = list[index]
                if (current is Map<*, *>) {
                    val mutable = current.toMutableMap()
                    mutable["url"] = url
                    mutable["title"] = title
                    list[index] = mutable
                    rtdb.getReference("Banner").setValue(list).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateBanner error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun softDeleteBanner(index: Int): Result<Unit> = setBannerHidden(index, true)
    suspend fun restoreBanner(index: Int): Result<Unit> = setBannerHidden(index, false)

    private suspend fun setBannerHidden(index: Int, hidden: Boolean): Result<Unit> {
        return try {
            val snapshot = rtdb.getReference("Banner").get().await()
            val rawData = snapshot.value
            if (rawData is List<*>) {
                val list = rawData.toMutableList()
                if (index < 0 || index >= list.size) return Result.failure(Exception("Index out of bounds"))
                val current = list[index]
                if (current is Map<*, *>) {
                    val mutable = current.toMutableMap()
                    mutable["isHidden"] = hidden
                    list[index] = mutable
                    rtdb.getReference("Banner").setValue(list).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "setBannerHidden error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── ORDER MANAGEMENT ────────────────────────────────────────

    suspend fun getAllOrders(): List<Order> {
        return try {
            val snapshot = firestore.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }
        } catch (e: Exception) {
            // Fallback: without orderBy if index not ready
            Log.w(TAG, "getAllOrders with sort failed, trying without: ${e.message}")
            try {
                val snapshot = firestore.collection("orders").get().await()
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                }.sortedByDescending { it.timestamp }
            } catch (e2: Exception) {
                Log.e(TAG, "getAllOrders fallback error: ${e2.message}")
                emptyList()
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("orders").document(orderId)
                .update("orderStatus", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderStatus error: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AdminRepository"
    }
}
