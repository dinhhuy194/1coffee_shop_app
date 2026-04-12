package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.R

class AdminBannerAdapter(
    private var items: List<Pair<Int, BannerModel>> = emptyList(), // (index, banner)
    private val onEdit: (Int, BannerModel) -> Unit,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<AdminBannerAdapter.ViewHolder>() {

    fun updateData(newItems: List<Pair<Int, BannerModel>>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (index, banner) = items[position]
        val isHidden = banner.isHidden

        holder.titleTxt.text = if (banner.title.isNullOrBlank()) "Banner ${index + 1}" else banner.title

        Glide.with(holder.itemView.context)
            .load(banner.url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imgBanner)

        holder.itemView.alpha = if (isHidden) 0.5f else 1f

        holder.btnToggle.setImageResource(
            if (isHidden) android.R.drawable.ic_menu_revert
            else android.R.drawable.ic_menu_close_clear_cancel
        )

        holder.btnEdit.setOnClickListener { onEdit(index, banner) }
        holder.btnToggle.setOnClickListener { onToggle(index, !isHidden) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgBanner: ImageView = view.findViewById(R.id.imgBanner)
        val titleTxt: TextView = view.findViewById(R.id.titleTxt)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnToggle: ImageView = view.findViewById(R.id.btnToggle)
    }
}
