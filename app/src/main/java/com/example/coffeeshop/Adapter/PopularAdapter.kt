package com.example.coffeeshop.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coffeeshop.Activity.DetailActivity
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.R
import com.example.coffeeshop.Repository.FavoriteRepository
import com.example.coffeeshop.databinding.ViewholderPopularBinding
import com.google.firebase.auth.FirebaseAuth

class PopularAdapter(val items:MutableList<ItemsModel>): RecyclerView.Adapter<PopularAdapter.ViewHolder>() {
    lateinit var context : Context
    private val favoriteRepository = FavoriteRepository()
    private val auth = FirebaseAuth.getInstance()
    
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
        val item = items[position]
        
        holder.binding.titleTxt.text = item.title
        holder.binding.priceTxt.text = "$" + item.price.toString()
        Glide.with(context)
            .load(item.picUrl[0])
            .into(holder.binding.pic)
        
        // Load favorite status from Firebase
        val userId = auth.currentUser?.uid
        if (userId != null) {
            favoriteRepository.isFavorite(userId, item.title) { isFavorite ->
                item.isFavorite = isFavorite
                updateFavoriteIcon(holder.binding, item)
            }
        } else {
            // No user logged in - reset to unfavorited
            item.isFavorite = false
            updateFavoriteIcon(holder.binding, item)
        }
        
        // Click favorite icon to toggle
        holder.binding.favoriteIcon.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                favoriteRepository.toggleFavorite(userId, item) { isFavorite ->
                    item.isFavorite = isFavorite
                    updateFavoriteIcon(holder.binding, item)
                    
                    val message = if (isFavorite) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Click whole item to view detail
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", item)
            context.startActivity(intent)
        }
        
        // Click addToCartBtn to view detail
        holder.binding.addToCartBtn.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", item)
            context.startActivity(intent)
        }
    }
    
    private fun updateFavoriteIcon(binding: ViewholderPopularBinding, item: ItemsModel) {
        val iconRes = if (item.isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_outline
        }
        binding.favoriteIcon.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = items.size
}