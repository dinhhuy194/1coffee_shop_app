package com.example.coffeeshop.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Review
import com.example.coffeeshop.R
import java.util.concurrent.TimeUnit

/**
 * Adapter hiển thị danh sách đánh giá sản phẩm.
 *
 * @param reviews        Danh sách reviews
 * @param currentUserId  UID user hiện tại (để highlight review của mình + hiện nút xóa)
 * @param showItemName   true = hiển thị tên sản phẩm (dùng ở MyReviewsActivity)
 * @param onLikeClick    Callback khi nhấn like
 * @param onDeleteClick  Callback khi nhấn xóa
 */
class ReviewAdapter(
    private var reviews: List<Review>,
    private val currentUserId: String?,
    private val showItemName: Boolean = false,
    private val onLikeClick: ((Review) -> Unit)? = null,
    private val onDeleteClick: ((Review) -> Unit)? = null
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarTxt: TextView = itemView.findViewById(R.id.avatarTxt)
        val userNameTxt: TextView = itemView.findViewById(R.id.userNameTxt)
        val timeTxt: TextView = itemView.findViewById(R.id.timeTxt)
        val deleteBtn: ImageView = itemView.findViewById(R.id.deleteBtn)
        val star1: ImageView = itemView.findViewById(R.id.star1)
        val star2: ImageView = itemView.findViewById(R.id.star2)
        val star3: ImageView = itemView.findViewById(R.id.star3)
        val star4: ImageView = itemView.findViewById(R.id.star4)
        val star5: ImageView = itemView.findViewById(R.id.star5)
        val itemNameTxt: TextView = itemView.findViewById(R.id.itemNameTxt)
        val commentTxt: TextView = itemView.findViewById(R.id.commentTxt)
        val likeLayout: LinearLayout = itemView.findViewById(R.id.likeLayout)
        val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        val likeCountTxt: TextView = itemView.findViewById(R.id.likeCountTxt)

        val stars get() = listOf(star1, star2, star3, star4, star5)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        val context = holder.itemView.context

        // Avatar (chữ cái đầu)
        val initial = review.userName.firstOrNull()?.uppercase() ?: "?"
        holder.avatarTxt.text = initial

        // Tên + thời gian
        holder.userNameTxt.text = review.userName
        holder.timeTxt.text = getRelativeTime(review.createdAt)

        // Sao
        holder.stars.forEachIndexed { index, iv ->
            val color = if (index < review.rating) 0xFFFFC107.toInt() else 0xFFD0C4BB.toInt()
            iv.setColorFilter(color)
        }

        // Tên sản phẩm (chỉ hiện ở MyReviews)
        if (showItemName) {
            holder.itemNameTxt.visibility = View.VISIBLE
            holder.itemNameTxt.text = "📦 ${review.itemId}"
        } else {
            holder.itemNameTxt.visibility = View.GONE
        }

        // Comment
        if (review.comment.isNotBlank()) {
            holder.commentTxt.visibility = View.VISIBLE
            holder.commentTxt.text = review.comment
        } else {
            holder.commentTxt.visibility = View.GONE
        }

        // Like
        val isLiked = currentUserId != null && review.likedBy.contains(currentUserId)
        holder.likeCountTxt.text = review.likes.toString()
        updateLikeAppearance(holder, context, isLiked)

        holder.likeLayout.setOnClickListener {
            onLikeClick?.invoke(review)
        }

        // Nút xóa (chỉ hiện cho chủ review)
        if (currentUserId != null && review.userId == currentUserId) {
            holder.deleteBtn.visibility = View.VISIBLE
            holder.deleteBtn.setOnClickListener {
                onDeleteClick?.invoke(review)
            }
        } else {
            holder.deleteBtn.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateList(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun updateLikeAppearance(holder: ViewHolder, context: Context, isLiked: Boolean) {
        if (isLiked) {
            holder.likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.orange))
            holder.likeCountTxt.setTextColor(ContextCompat.getColor(context, R.color.orange))
        } else {
            holder.likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.grey))
            holder.likeCountTxt.setTextColor(ContextCompat.getColor(context, R.color.grey))
        }
    }

    companion object {
        /**
         * Chuyển timestamp thành chuỗi tương đối: "Vừa xong", "5 phút trước", "2 ngày trước"...
         */
        fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours   = TimeUnit.MILLISECONDS.toHours(diff)
            val days    = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1  -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24   -> "$hours giờ trước"
                days < 30    -> "$days ngày trước"
                days < 365   -> "${days / 30} tháng trước"
                else         -> "${days / 365} năm trước"
            }
        }
    }
}
