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
import com.example.coffeeshop.databinding.ViewholderItemPicLeftBinding
import com.example.coffeeshop.databinding.ViewholderItemPicRightBinding
import com.google.firebase.auth.FirebaseAuth

class ItemsListCategoryAdapter(val items:MutableList<ItemsModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object{
        const val TYPE_ITEM1 = 0
        const val TYPE_ITEM2 = 1
    }

    private lateinit var context : Context
    private val favoriteRepository = FavoriteRepository()
    private val auth = FirebaseAuth.getInstance()
    
    override fun getItemViewType(position: Int): Int {
        return if(position % 2 == 0) TYPE_ITEM1
        else TYPE_ITEM2
    }

    class ViewholderItem1(val binding: ViewholderItemPicRightBinding): RecyclerView.ViewHolder(binding.root)
    class ViewholderItem2(val binding: ViewholderItemPicLeftBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        context = parent.context
        return when (viewType){
            TYPE_ITEM1 -> {
                val binding = ViewholderItemPicRightBinding.inflate(LayoutInflater.from(context),parent,false)
                ViewholderItem1(binding)
            }
            else -> {
                val binding = ViewholderItemPicLeftBinding.inflate(LayoutInflater.from(context),parent,false)
                ViewholderItem2(binding)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        when (holder) {
            is ViewholderItem1 -> {
                holder.binding.titleTxt.text = item.title
                holder.binding.priceTxt.text = "$" + item.price.toString()
                holder.binding.ratingBar.rating = item.rating.toFloat()

                Glide.with(context)
                    .load(item.picUrl[0])
                    .into(holder.binding.picMain)

                // Load favorite status from Firebase
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    favoriteRepository.isFavorite(userId, item.title) { isFavorite ->
                        item.isFavorite = isFavorite
                        updateFavoriteIcon1(holder.binding, item)
                    }
                } else {
                    // No user logged in - reset to unfavorited
                    item.isFavorite = false
                    updateFavoriteIcon1(holder.binding, item)
                }
                
                // Click favorite icon to toggle
                holder.binding.favoriteIcon.setOnClickListener {
                    handleFavoriteClick(item) { isFavorite ->
                        item.isFavorite = isFavorite
                        updateFavoriteIcon1(holder.binding, item)
                    }
                }
                
                holder.itemView.setOnClickListener {
                    val intent = Intent(context, DetailActivity::class.java)
                    intent.putExtra("object", item)
                    context.startActivity(intent)
                }
            }
            is ViewholderItem2 -> {
                holder.binding.titleTxt.text = item.title
                holder.binding.priceTxt.text = "$" + item.price.toString()
                holder.binding.ratingBar.rating = item.rating.toFloat()

                Glide.with(context)
                    .load(item.picUrl[0])
                    .into(holder.binding.picMain)

                // Load favorite status from Firebase
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    favoriteRepository.isFavorite(userId, item.title) { isFavorite ->
                        item.isFavorite = isFavorite
                        updateFavoriteIcon2(holder.binding, item)
                    }
                } else {
                    // No user logged in - reset to unfavorited
                    item.isFavorite = false
                    updateFavoriteIcon2(holder.binding, item)
                }
                
                // Click favorite icon to toggle
                holder.binding.favoriteIcon.setOnClickListener {
                    handleFavoriteClick(item) { isFavorite ->
                        item.isFavorite = isFavorite
                        updateFavoriteIcon2(holder.binding, item)
                    }
                }
                
                holder.itemView.setOnClickListener {
                    val intent = Intent(context, DetailActivity::class.java)
                    intent.putExtra("object", item)
                    context.startActivity(intent)
                }
            }
        }
    }
    
    private fun handleFavoriteClick(item: ItemsModel, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            favoriteRepository.toggleFavorite(userId, item) { isFavorite ->
                callback(isFavorite)
                val message = if (isFavorite) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateFavoriteIcon1(binding: ViewholderItemPicRightBinding, item: ItemsModel) {
        val iconRes = if (item.isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_outline
        }
        binding.favoriteIcon.setImageResource(iconRes)
    }
    
    private fun updateFavoriteIcon2(binding: ViewholderItemPicLeftBinding, item: ItemsModel) {
        val iconRes = if (item.isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_outline
        }
        binding.favoriteIcon.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = items.size
}