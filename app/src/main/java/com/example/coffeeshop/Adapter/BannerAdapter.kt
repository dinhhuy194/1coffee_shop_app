package com.example.coffeeshop.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.R

/**
 * BannerAdapter — RecyclerView.Adapter cho ViewPager2 banner slider.
 * Hiển thị ảnh (Glide) + title overlay nếu có.
 */
class BannerAdapter(private val banners: List<BannerModel>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private lateinit var context: Context

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bannerImage: ImageView = itemView.findViewById(R.id.bannerImage)
        val bannerTitle: TextView = itemView.findViewById(R.id.bannerTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.viewholder_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val banner = banners[position]

        Glide.with(context)
            .load(banner.url)
            .transform(CenterCrop(), RoundedCorners(40))
            .placeholder(R.drawable.cf_background)
            .error(R.drawable.cf_background)
            .into(holder.bannerImage)

        if (banner.title.isNotEmpty()) {
            holder.bannerTitle.text = banner.title
            holder.bannerTitle.visibility = View.VISIBLE
        } else {
            holder.bannerTitle.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = banners.size
}
