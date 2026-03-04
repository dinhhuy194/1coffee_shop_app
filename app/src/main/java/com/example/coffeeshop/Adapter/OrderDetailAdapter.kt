package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.OrderItem
import com.example.coffeeshop.databinding.ItemOrderDetailBinding

class OrderDetailAdapter(private val items: List<OrderItem>) : RecyclerView.Adapter<OrderDetailAdapter.ViewHolder>() {
    
    inner class ViewHolder(val binding: ItemOrderDetailBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.binding.apply {
            itemTitleTxt.text = item.title
            
            // Build details string (Size • Ice • Sugar)
            val details = listOfNotNull(
                item.selectedSize.takeIf { it.isNotEmpty() },
                item.iceOption.takeIf { it.isNotEmpty() },
                item.sugarOption.takeIf { it.isNotEmpty() }
            ).joinToString(" • ")
            
            itemDetailsTxt.text = details
            itemQuantityTxt.text = "x${item.quantity}"
            itemPriceTxt.text = "$${String.format("%.2f", item.price * item.quantity)}"
        }
    }
    
    override fun getItemCount() = items.size
}
