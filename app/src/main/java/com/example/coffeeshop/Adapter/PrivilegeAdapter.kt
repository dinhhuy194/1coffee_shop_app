package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Privilege
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.databinding.ItemPrivilegeBinding

class PrivilegeAdapter(
    private var userRank: String = User.RANK_NORMAL
) : ListAdapter<Privilege, PrivilegeAdapter.ViewHolder>(DiffCallback()) {

    fun updateRank(rank: String) {
        userRank = rank
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPrivilegeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), userRank)
    }

    inner class ViewHolder(private val binding: ItemPrivilegeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(privilege: Privilege, userRank: String) {
            binding.apply {
                privilegeIcon.text = privilege.iconEmoji
                privilegeTitle.text = privilege.title
                privilegeDescription.text = privilege.description

                // Badge hạng yêu cầu
                privilegeRankBadge.text = privilege.requiredRank
                val badgeColor = getRankColor(privilege.requiredRank)
                privilegeRankBadge.setBackgroundColor(badgeColor)

                // Alpha: mờ nếu user chưa đạt hạng yêu cầu
                val alpha = privilege.getAlphaForUser(userRank)
                privilegeCard.alpha = alpha
            }
        }

        private fun getRankColor(rank: String): Int {
            val ctx = binding.root.context
            return when (rank) {
                User.RANK_SILVER  -> ctx.getColor(com.example.coffeeshop.R.color.rankSilver)
                User.RANK_GOLD    -> ctx.getColor(com.example.coffeeshop.R.color.rankGold)
                User.RANK_DIAMOND -> ctx.getColor(com.example.coffeeshop.R.color.rankDiamond)
                else              -> ctx.getColor(com.example.coffeeshop.R.color.rankNormal)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Privilege>() {
        override fun areItemsTheSame(oldItem: Privilege, newItem: Privilege) =
            oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: Privilege, newItem: Privilege) =
            oldItem == newItem
    }
}
