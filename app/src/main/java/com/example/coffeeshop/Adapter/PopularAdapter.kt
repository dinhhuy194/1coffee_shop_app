package com.example.coffeeshop.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coffeeshop.Activity.DetailActivity
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.databinding.ViewholderPopularBinding

class PopularAdapter(val items:MutableList<ItemsModel>): RecyclerView.Adapter<PopularAdapter.ViewHolder>() {
    lateinit var context : Context
    class ViewHolder(val binding: ViewholderPopularBinding): RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PopularAdapter.ViewHolder {
        context = parent.context
        val binding = ViewholderPopularBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularAdapter.ViewHolder, position: Int) {
        holder.binding.titleTxt.text = items[position].title
        holder.binding.priceTxt.text = "$" + items[position].price.toString()
        Glide.with(context)
            .load(items[position].picUrl[0])
            .into(holder.binding.pic)
        
        // Click whole item to view detail
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            context.startActivity(intent)
        }
        
        // Click addToCartBtn to view detail
        holder.binding.addToCartBtn.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}