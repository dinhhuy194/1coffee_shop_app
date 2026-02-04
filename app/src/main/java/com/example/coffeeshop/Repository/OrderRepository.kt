package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Model.OrderItem
import com.example.coffeeshop.Domain.ItemsModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Create new order in Firestore
     */
    suspend fun createOrder(
        userId: String,
        items: List<ItemsModel>,
        subtotal: Double,
        tax: Double,
        delivery: Double,
        totalAmount: Double
    ): Result<String> {
        return try {
            val orderId = "ORD_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            
            val orderItems = items.map { item ->
                OrderItem(
                    title = item.title,
                    price = item.price,
                    quantity = item.numberInCart,
                    selectedSize = item.selectedSize,
                    iceOption = item.iceOption,
                    sugarOption = item.sugarOption
                )
            }
            
            val order = Order(
                orderId = orderId,
                userId = userId,
                items = orderItems,
                subtotal = subtotal,
                tax = tax,
                delivery = delivery,
                totalAmount = totalAmount,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            
            firestore.collection("orders")
                .document(orderId)
                .set(order)
                .await()
            
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get order history for a user
     */
    suspend fun getOrderHistory(userId: String): Result<List<Order>> {
        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Convert to objects and sort by timestamp descending
            val orders = snapshot.toObjects(Order::class.java)
                .sortedByDescending { it.timestamp }
            
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
