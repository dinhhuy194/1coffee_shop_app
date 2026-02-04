package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {
    
    inner class ViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        
        holder.binding.apply {
            orderIdTxt.text = order.orderId
            totalAmountTxt.text = "$${String.format("%.2f", order.totalAmount)}"
            
            // Format timestamp to readable date
            val date = Date(order.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTxt.text = dateFormat.format(date)
            
            // Item count
            val itemCount = order.items.sumOf { it.quantity }
            itemCountTxt.text = "$itemCount items"
            
            // Status
            statusTxt.text = order.status.uppercase()
        }
    }
    
    override fun getItemCount() = orders.size
}
