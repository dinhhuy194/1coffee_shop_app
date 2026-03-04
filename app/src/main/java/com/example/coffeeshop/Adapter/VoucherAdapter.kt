package com.example.coffeeshop.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.BeanVoucher
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ItemVoucherBinding

class VoucherAdapter(
    private var userPoints: Long = 0L,
    private val onRedeem: (BeanVoucher) -> Unit
) : ListAdapter<BeanVoucher, VoucherAdapter.ViewHolder>(DiffCallback()) {

    fun updateUserPoints(points: Long) {
        userPoints = points
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVoucherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), userPoints)
    }

    inner class ViewHolder(private val binding: ItemVoucherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: BeanVoucher, userPoints: Long) {
            binding.apply {
                voucherIcon.text = voucher.iconEmoji
                voucherName.text = voucher.name
                voucherDescription.text = voucher.description
                voucherBeanCost.text = "${voucher.beanCost} BEAN"

                val canAfford = userPoints >= voucher.beanCost

                if (canAfford) {
                    // Đủ điểm: nút sáng, click được
                    btnRedeem.isEnabled = true
                    btnRedeem.alpha = 1.0f
                    btnRedeem.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            root.context.getColor(R.color.orange)
                        )
                    btnRedeem.text = "Đổi"
                    txtNotEnough.visibility = View.GONE
                } else {
                    // Không đủ điểm: nút xám, không click được
                    btnRedeem.isEnabled = false
                    btnRedeem.alpha = 0.5f
                    btnRedeem.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(Color.LTGRAY)
                    btnRedeem.text = "Đổi"
                    val deficit = voucher.beanCost - userPoints
                    txtNotEnough.text = "Thiếu $deficit BEAN"
                    txtNotEnough.visibility = View.VISIBLE
                }

                btnRedeem.setOnClickListener {
                    if (canAfford) onRedeem(voucher)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BeanVoucher>() {
        override fun areItemsTheSame(oldItem: BeanVoucher, newItem: BeanVoucher) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BeanVoucher, newItem: BeanVoucher) =
            oldItem == newItem
    }
}
