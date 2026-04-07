package com.example.coffeeshop.Repository

import android.util.Log
import com.example.coffeeshop.Domain.ItemsModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FavoriteRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val favoritesCollection = db.collection("favorites")
    
    /**
     * Add item to user's favorites in Firestore
     */
    fun addFavorite(userId: String, item: ItemsModel, onComplete: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            Log.e("FavoriteRepository", "User not logged in")
            onComplete(false)
            return
        }
        
        val favoriteData = hashMapOf(
            "title" to item.title,
            "description" to item.description,
            "picUrl" to item.picUrl,
            "price" to item.price,
            "rating" to item.rating,
            "categoryId" to item.categoryId
        )
        
        favoritesCollection.document(userId)
            .update("items", FieldValue.arrayUnion(favoriteData))
            .addOnSuccessListener {
                Log.d("FavoriteRepository", "Item added to favorites")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                // Document might not exist, try to create it
                favoritesCollection.document(userId)
                    .set(hashMapOf("items" to listOf(favoriteData)))
                    .addOnSuccessListener {
                        Log.d("FavoriteRepository", "Favorites document created and item added")
                        onComplete(true)
                    }
                    .addOnFailureListener { error ->
                        Log.e("FavoriteRepository", "Error adding favorite: ${error.message}")
                        onComplete(false)
                    }
            }
    }
    
    /**
     * Remove item from user's favorites in Firestore
     */
    fun removeFavorite(userId: String, item: ItemsModel, onComplete: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            Log.e("FavoriteRepository", "User not logged in")
            onComplete(false)
            return
        }
        
        // First, get all favorites to find the exact match
        getFavorites(userId) { favorites ->
            val itemToRemove = favorites.find { it.title == item.title }
            
            if (itemToRemove != null) {
                val favoriteData = hashMapOf(
                    "title" to itemToRemove.title,
                    "description" to itemToRemove.description,
                    "picUrl" to itemToRemove.picUrl,
                    "price" to itemToRemove.price,
                    "rating" to itemToRemove.rating,
                    "categoryId" to itemToRemove.categoryId
                )
                
                favoritesCollection.document(userId)
                    .update("items", FieldValue.arrayRemove(favoriteData))
                    .addOnSuccessListener {
                        Log.d("FavoriteRepository", "Item removed from favorites")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoriteRepository", "Error removing favorite: ${e.message}")
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }
    
    /**
     * Get all favorite items for a user
     */
    fun getFavorites(userId: String, onComplete: (List<ItemsModel>) -> Unit) {
        if (userId.isEmpty()) {
            Log.e("FavoriteRepository", "User not logged in")
            onComplete(emptyList())
            return
        }
        
        favoritesCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val items = document.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                    val favoritesList = items.map { item ->
                        ItemsModel(
                            title = item["title"] as? String ?: "",
                            description = item["description"] as? String ?: "",
                            picUrl = item["picUrl"] as? ArrayList<String> ?: ArrayList(),
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            rating = (item["rating"] as? Number)?.toDouble() ?: 0.0,
                            categoryId = item["categoryId"] as? String ?: "",
                            isFavorite = true
                        )
                    }
                    onComplete(favoritesList)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("FavoriteRepository", "Error getting favorites: ${e.message}")
                onComplete(emptyList())
            }
    }
    
    /**
     * Check if an item is in user's favorites
     */
    fun isFavorite(userId: String, itemTitle: String, onComplete: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            onComplete(false)
            return
        }
        
        getFavorites(userId) { favorites ->
            val isFav = favorites.any { it.title == itemTitle }
            onComplete(isFav)
        }
    }
    
    /**
     * Toggle favorite status of an item
     */
    fun toggleFavorite(userId: String, item: ItemsModel, onComplete: (Boolean) -> Unit) {
        isFavorite(userId, item.title) { isFav ->
            if (isFav) {
                removeFavorite(userId, item) { success ->
                    onComplete(!success) // Return false if removed successfully
                }
            } else {
                addFavorite(userId, item) { success ->
                    onComplete(success) // Return true if added successfully
                }
            }
        }
    }
    
    /**
     * Clear all favorites for a user
     */
    fun clearFavorites(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            onComplete(false)
            return
        }
        
        favoritesCollection.document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d("FavoriteRepository", "All favorites cleared")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FavoriteRepository", "Error clearing favorites: ${e.message}")
                onComplete(false)
            }
    }
}
