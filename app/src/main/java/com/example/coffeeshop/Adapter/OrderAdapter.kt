package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {
    
    // Track expanded state for each order
    private val expandedStates = mutableMapOf<String, Boolean>()
    
    inner class ViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        val isExpanded = expandedStates[order.orderId] ?: false
        
        holder.binding.apply {
            // Order ID
            orderIdTxt.text = order.orderId
            
            // Total amount
            totalAmountTxt.text = "$${String.format("%.2f", order.totalAmount)}"
            
            // Format timestamp to readable date
            val date = Date(order.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTxt.text = dateFormat.format(date)
            
            // Item count
            val itemCount = order.items.sumOf { it.quantity }
            val itemText = if (itemCount == 1) "1 item" else "$itemCount items"
            itemCountTxt.text = itemText
            
            // Status with color coding
            statusTxt.text = order.status.uppercase()
            when (order.status.lowercase()) {
                "pending" -> {
                    statusTxt.setBackgroundResource(R.drawable.bg_status_pending)
                }
                "completed" -> {
                    statusTxt.setBackgroundResource(R.drawable.bg_status_completed)
                }
                "cancelled" -> {
                    statusTxt.setBackgroundResource(R.drawable.bg_status_cancelled)
                }
                else -> {
                    statusTxt.setBackgroundResource(R.drawable.bg_status_pending)
                }
            }
            
            // Expand/collapse icon with animation
            val rotation = if (isExpanded) 180f else 0f
            expandIcon.animate()
                .rotation(rotation)
                .setDuration(200)
                .start()
            
            expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less 
                else R.drawable.ic_expand_more
            )
            
            // Show/hide items RecyclerView
            if (isExpanded) {
                divider.visibility = View.VISIBLE
                itemsRecyclerView.visibility = View.VISIBLE
                
                // Setup nested RecyclerView for order items
                itemsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                itemsRecyclerView.adapter = OrderItemAdapter(order.items)
            } else {
                divider.visibility = View.GONE
                itemsRecyclerView.visibility = View.GONE
            }
            
            // Click listener for expand/collapse
            orderHeaderLayout.setOnClickListener {
                expandedStates[order.orderId] = !isExpanded
                notifyItemChanged(position)
            }
        }
    }
    
    override fun getItemCount() = orders.size
}
