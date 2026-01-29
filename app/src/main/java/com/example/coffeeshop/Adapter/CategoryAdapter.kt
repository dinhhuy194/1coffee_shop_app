package com.example.coffeeshop.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ViewholderCategoryBinding

class CategoryAdapter(val items:MutableList<CategoryModel>): RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private lateinit var context: Context
    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    inner class ViewHolder(val binding: ViewholderCategoryBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryAdapter.ViewHolder {
        context = parent.context
        val binding = ViewholderCategoryBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CategoryAdapter.ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleCat.text = item.title
        holder.binding.root.setOnClickListener{
            selectedPosition = position
            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(selectedPosition)
            lastSelectedPosition = selectedPosition
        }
        if (selectedPosition == position){
            holder.binding.root.setBackgroundResource(R.drawable.dark_brown_bg)
            holder.binding.titleCat.setTextColor(context.resources.getColor(R.color.white))
        }else{
            holder.binding.root.setBackgroundResource(R.drawable.white_bg)
            holder.binding.titleCat.setTextColor(context.resources.getColor(R.color.darkBrown))
        }
    }

    override fun getItemCount(): Int = items.size

}