package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Register with email and password
     */
    suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("User creation failed"))
            
            val user =User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                photoUrl = null,
                createdAt = System.currentTimeMillis()
            )
            
            // Create user document in Firestore
            createUserDocument(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Login failed"))
            
            // Get user from Firestore
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                    ?: return Result.failure(Exception("User data not found"))
                Result.success(user)
            } else {
                // Fallback: create basic user object
                val user = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Google sign-in failed"))
            
            val user = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                createdAt = System.currentTimeMillis()
            )
            
            // Check if user document exists, if not create it
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            if (!userDoc.exists()) {
                createUserDocument(user)
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create user document in Firestore
     */
    private suspend fun createUserDocument(user: User): Result<Unit> {
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
     * Get current user
     */
    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        
        return try {
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
