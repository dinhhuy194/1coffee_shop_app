package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.PointHistory
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ItemPointHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PointHistoryAdapter : ListAdapter<PointHistory, PointHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPointHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPointHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: PointHistory) {
            val ctx = binding.root.context
            binding.apply {
                historyReason.text = history.reason
                historyTime.text = dateFormat.format(Date(history.timestamp))
                historyDelta.text = history.formattedDelta

                if (history.isPositive) {
                    historyDelta.setTextColor(ctx.getColor(R.color.pointPositive))
                    historyIconBg.setBackgroundColor(ctx.getColor(R.color.pointPositiveBg))
                    historyIconBg.text = "🫘"
                } else {
                    historyDelta.setTextColor(ctx.getColor(R.color.pointNegative))
                    historyIconBg.setBackgroundColor(ctx.getColor(R.color.pointNegativeBg))
                    historyIconBg.text = "🎁"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PointHistory>() {
        override fun areItemsTheSame(oldItem: PointHistory, newItem: PointHistory) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PointHistory, newItem: PointHistory) =
            oldItem == newItem
    }
}
