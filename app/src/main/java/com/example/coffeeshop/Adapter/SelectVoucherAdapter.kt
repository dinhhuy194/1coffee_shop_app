package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.RedeemedVoucher
import com.example.coffeeshop.R
import com.google.android.material.card.MaterialCardView

/**
 * Adapter hiển thị danh sách voucher chưa dùng trong bottom sheet dialog.
 * Hỗ trợ chọn 1 voucher (single selection) kiểu radio button.
 *
 * @param vouchers          Danh sách voucher chưa sử dụng
 * @param selectedVoucherId ID của voucher đang được chọn (rỗng nếu chưa chọn)
 * @param onVoucherSelected Callback khi người dùng chọn voucher
 */
class SelectVoucherAdapter(
    private val vouchers: List<RedeemedVoucher>,
    private var selectedVoucherId: String = "",
    private val onVoucherSelected: (RedeemedVoucher) -> Unit
) : RecyclerView.Adapter<SelectVoucherAdapter.VoucherViewHolder>() {

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.voucherSelectCard)
        val icon: TextView = itemView.findViewById(R.id.selectVoucherIcon)
        val name: TextView = itemView.findViewById(R.id.selectVoucherName)
        val discount: TextView = itemView.findViewById(R.id.selectVoucherDiscount)
        val desc: TextView = itemView.findViewById(R.id.selectVoucherDesc)
        val radio: RadioButton = itemView.findViewById(R.id.selectVoucherRadio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_voucher, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val voucher = vouchers[position]
        val isSelected = voucher.id == selectedVoucherId

        holder.icon.text = voucher.iconEmoji
        holder.name.text = voucher.name
        holder.discount.text = voucher.discountLabel
        holder.desc.text = voucher.description
        holder.radio.isChecked = isSelected

        // Highlight card khi được chọn
        holder.card.apply {
            strokeColor = if (isSelected) {
                context.getColor(R.color.orange)
            } else {
                android.graphics.Color.TRANSPARENT
            }
            strokeWidth = if (isSelected) 4 else 0
        }

        holder.card.setOnClickListener {
            val prev = selectedVoucherId
            selectedVoucherId = voucher.id
            // Refresh item cũ (bỏ check) và item mới (check)
            val prevPos = vouchers.indexOfFirst { it.id == prev }
            if (prevPos >= 0) notifyItemChanged(prevPos)
            notifyItemChanged(position)
            onVoucherSelected(voucher)
        }
    }

    override fun getItemCount(): Int = vouchers.size

    /** Cập nhật selection từ bên ngoài (vd: bỏ chọn) */
    fun clearSelection() {
        val prev = selectedVoucherId
        selectedVoucherId = ""
        val prevPos = vouchers.indexOfFirst { it.id == prev }
        if (prevPos >= 0) notifyItemChanged(prevPos)
    }
}
