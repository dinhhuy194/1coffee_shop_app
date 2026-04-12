package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.R

class AdminCategoryAdapter(
    private var items: List<CategoryModel> = emptyList(),
    private val onEdit: (CategoryModel) -> Unit,
    private val onToggle: (CategoryModel, Boolean) -> Unit,
    private var showHidden: Boolean = false
) : RecyclerView.Adapter<AdminCategoryAdapter.ViewHolder>() {

    // Map id → isHidden (maintained externally via raw data)
    private var hiddenMap = mutableMapOf<Int, Boolean>()

    fun updateData(allItems: List<CategoryModel>, hiddenStates: Map<Int, Boolean>, showHiddenTab: Boolean) {
        this.hiddenMap = hiddenStates.toMutableMap()
        this.showHidden = showHiddenTab
        this.items = allItems.filter { (hiddenStates[it.id] ?: false) == showHiddenTab }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isHidden = hiddenMap[item.id] ?: false

        holder.titleTxt.text = item.title
        holder.idTxt.text = "ID: ${item.id}"

        // Change toggle icon based on hidden state
        holder.btnToggle.setImageResource(
            if (isHidden) android.R.drawable.ic_menu_revert
            else android.R.drawable.ic_menu_close_clear_cancel
        )

        // Dim title if hidden
        holder.titleTxt.alpha = if (isHidden) 0.5f else 1f

        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnToggle.setOnClickListener { onToggle(item, !isHidden) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTxt: TextView = view.findViewById(R.id.titleTxt)
        val idTxt: TextView = view.findViewById(R.id.idTxt)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnToggle: ImageView = view.findViewById(R.id.btnToggle)
    }
}
