package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Get user profile from Firestore
     */
    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            val user = document.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
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
}
