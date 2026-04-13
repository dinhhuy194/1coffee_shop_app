package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.OrderItem
import com.example.coffeeshop.databinding.ItemOrderDetailBinding

class OrderItemAdapter(private val items: List<OrderItem>) : RecyclerView.Adapter<OrderItemAdapter.ViewHolder>() {
    
    inner class ViewHolder(val binding: ItemOrderDetailBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.binding.apply {
            // Item title
            itemTitleTxt.text = item.title
            
            // Item details (size, ice, sugar)
            val details = buildString {
                if (item.selectedSize.isNotEmpty()) append(item.selectedSize)
                if (item.iceOption.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(item.iceOption)
                }
                if (item.sugarOption.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(item.sugarOption)
                }
            }
            itemDetailsTxt.text = details
            
            // Quantity
            itemQuantityTxt.text = "x${item.quantity}"
            
            // Price (total for this item)
            val itemTotal = item.price * item.quantity
            itemPriceTxt.text = com.example.coffeeshop.Helper.CurrencyFormatter.format(itemTotal)
        }
    }
    
    override fun getItemCount() = items.size
}
