package com.example.coffeeshop.Helper

import android.content.Context
import android.widget.Toast
import com.example.coffeeshop.Domain.ItemsModel

class ManagmentFavorite(val context: Context) {
    
    private val tinyDB = TinyDB(context)
    
    /**
     * Add item to favorites list
     */
    fun addToFavorites(item: ItemsModel) {
        val favoritesList = getFavoritesList()
        
        // Check if already exists
        val exists = favoritesList.any { it.title == item.title }
        
        if (!exists) {
            favoritesList.add(item)
            tinyDB.putListObject("FavoriteList", favoritesList)
            Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Remove item from favorites list
     */
    fun removeFromFavorites(item: ItemsModel) {
        val favoritesList = getFavoritesList()
        val index = favoritesList.indexOfFirst { it.title == item.title }
        
        if (index != -1) {
            favoritesList.removeAt(index)
            tinyDB.putListObject("FavoriteList", favoritesList)
            Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get all favorite items
     */
    fun getFavoritesList(): ArrayList<ItemsModel> {
        return tinyDB.getListObject("FavoriteList") ?: arrayListOf()
    }
    
    /**
     * Check if item is in favorites
     */
    fun isFavorite(item: ItemsModel): Boolean {
        val favoritesList = getFavoritesList()
        return favoritesList.any { it.title == item.title }
    }
    
    /**
     * Toggle favorite status of an item
     */
    fun toggleFavorite(item: ItemsModel) {
        if (isFavorite(item)) {
            removeFromFavorites(item)
        } else {
            addToFavorites(item)
        }
    }
    
    /**
     * Clear all favorites
     */
    fun clearFavorites() {
        tinyDB.putListObject("FavoriteList", arrayListOf<ItemsModel>())
        Toast.makeText(context, "All favorites cleared", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Get favorites count
     */
    fun getFavoritesCount(): Int {
        return getFavoritesList().size
    }
}
