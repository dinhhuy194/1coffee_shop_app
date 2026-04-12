package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.R

class AdminItemAdapter(
    private var items: List<Pair<Int, ItemsModel>> = emptyList(), // (index, item)
    private val onEdit: (Int, ItemsModel) -> Unit,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<AdminItemAdapter.ViewHolder>() {

    fun updateData(newItems: List<Pair<Int, ItemsModel>>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (index, item) = items[position]
        val isHidden = item.isHidden

        holder.titleTxt.text = item.title
        holder.priceTxt.text = "$${item.price}"
        holder.ratingTxt.text = "⭐ ${item.rating}"
        holder.categoryTxt.text = "Category: ${item.categoryId}"

        // Load thumbnail
        if (item.picUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.picUrl[0])
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgThumbnail)
        }

        // Dim if hidden
        holder.itemView.alpha = if (isHidden) 0.5f else 1f

        holder.btnToggle.setImageResource(
            if (isHidden) android.R.drawable.ic_menu_revert
            else android.R.drawable.ic_menu_close_clear_cancel
        )

        holder.btnEdit.setOnClickListener { onEdit(index, item) }
        holder.btnToggle.setOnClickListener { onToggle(index, !isHidden) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val titleTxt: TextView = view.findViewById(R.id.titleTxt)
        val priceTxt: TextView = view.findViewById(R.id.priceTxt)
        val ratingTxt: TextView = view.findViewById(R.id.ratingTxt)
        val categoryTxt: TextView = view.findViewById(R.id.categoryTxt)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnToggle: ImageView = view.findViewById(R.id.btnToggle)
    }
}
