package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.RedeemedVoucher
import com.example.coffeeshop.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RedeemedVoucherAdapter(
    private var vouchers: List<RedeemedVoucher>,
    private val onUseVoucher: (RedeemedVoucher) -> Unit
) : RecyclerView.Adapter<RedeemedVoucherAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val voucherIcon: TextView = view.findViewById(R.id.voucherIcon)
        val voucherName: TextView = view.findViewById(R.id.voucherName)
        val voucherDescription: TextView = view.findViewById(R.id.voucherDescription)
        val voucherDate: TextView = view.findViewById(R.id.voucherDate)
        val statusBadge: TextView = view.findViewById(R.id.statusBadge)
        val useBtn: MaterialButton = view.findViewById(R.id.useBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_redeemed_voucher, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voucher = vouchers[position]
        val context = holder.itemView.context

        holder.voucherIcon.text = voucher.iconEmoji
        holder.voucherName.text = voucher.name
        holder.voucherDescription.text = voucher.description

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.voucherDate.text = "Đổi: ${dateFormat.format(Date(voucher.redeemedAt))}"

        if (voucher.isUsed) {
            // Đã sử dụng
            holder.statusBadge.text = "Đã dùng"
            holder.statusBadge.setBackgroundResource(R.drawable.bg_membership_card_normal)
            holder.useBtn.visibility = View.GONE
            holder.itemView.alpha = 0.5f
            
            // Hiển thị ngày sử dụng
            if (voucher.usedAt > 0) {
                holder.voucherDate.text = "Dùng: ${dateFormat.format(Date(voucher.usedAt))}"
            }
        } else {
            // Chưa sử dụng
            holder.statusBadge.text = "Chưa dùng"
            holder.statusBadge.setBackgroundResource(R.drawable.bg_membership_card_gold)
            holder.useBtn.visibility = View.VISIBLE
            holder.itemView.alpha = 1.0f

            holder.useBtn.setOnClickListener {
                onUseVoucher(voucher)
            }
        }
    }

    override fun getItemCount() = vouchers.size

    fun updateList(newList: List<RedeemedVoucher>) {
        vouchers = newList
        notifyDataSetChanged()
    }
}
