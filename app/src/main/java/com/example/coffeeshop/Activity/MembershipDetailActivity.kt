package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.PointHistoryAdapter
import com.example.coffeeshop.Adapter.PrivilegeAdapter
import com.example.coffeeshop.Adapter.VoucherAdapter
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.MembershipViewModel
import com.example.coffeeshop.databinding.ActivityMembershipDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class MembershipDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMembershipDetailBinding
    private val viewModel: MembershipViewModel by viewModels()

    private lateinit var privilegeAdapter: PrivilegeAdapter
    private lateinit var voucherAdapter: VoucherAdapter
    private lateinit var historyAdapter: PointHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembershipDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupTabLayout()
        setupListeners()
        observeViewModel()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadMembershipData(userId)
        }
    }

    private fun setupAdapters() {
        privilegeAdapter = PrivilegeAdapter()
        voucherAdapter = VoucherAdapter(userPoints = 0L) { voucher ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@VoucherAdapter
            viewModel.redeemVoucher(userId, voucher)
        }
        historyAdapter = PointHistoryAdapter()

        binding.rvPrivileges.apply {
            layoutManager = LinearLayoutManager(this@MembershipDetailActivity)
            adapter = privilegeAdapter
        }
        binding.rvVouchers.apply {
            layoutManager = LinearLayoutManager(this@MembershipDetailActivity)
            adapter = voucherAdapter
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MembershipDetailActivity)
            adapter = historyAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTab(Tab.PRIVILEGES)
                    1 -> showTab(Tab.VOUCHERS)
                    2 -> showTab(Tab.HISTORY)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTab(tab: Tab) {
        binding.rvPrivileges.visibility   = if (tab == Tab.PRIVILEGES) View.VISIBLE else View.GONE
        binding.rvVouchers.visibility     = if (tab == Tab.VOUCHERS)   View.VISIBLE else View.GONE
        binding.historyContainer.visibility = if (tab == Tab.HISTORY)  View.VISIBLE else View.GONE
    }

    private enum class Tab { PRIVILEGES, VOUCHERS, HISTORY }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.userProfile.observe(this) { user ->
            user ?: return@observe
            updateHeaderUI(user)
            privilegeAdapter.updateRank(user.rank)
            voucherAdapter.updateUserPoints(user.totalPoints)
        }

        viewModel.privileges.observe(this) { list ->
            privilegeAdapter.submitList(list)
        }

        viewModel.vouchers.observe(this) { list ->
            voucherAdapter.submitList(list)
        }

        viewModel.pointHistory.observe(this) { history ->
            historyAdapter.submitList(history)
            // Show/hide empty state
            val isEmpty = history.isEmpty()
            binding.rvHistory.visibility  = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyHistory.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        viewModel.redeemState.observe(this) { state ->
            when (state) {
                is MembershipViewModel.RedeemState.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                }
                is MembershipViewModel.RedeemState.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetRedeemState()
                    // Refresh vouchers với điểm mới
                    val currentPoints = viewModel.userProfile.value?.totalPoints ?: 0L
                    voucherAdapter.updateUserPoints(currentPoints)
                }
                is MembershipViewModel.RedeemState.Error -> {
                    binding.loadingProgress.visibility = View.GONE
                    Snackbar.make(binding.root, "❌ ${state.message}", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.pointNegative))
                        .show()
                    viewModel.resetRedeemState()
                }
                else -> {
                    binding.loadingProgress.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Cập nhật header: gradient background, bean count, rank name, progress bar.
     * Progress bar dựa trên lifetimePoints (tổng BEAN tích lũy, không bị trừ khi đổi quà).
     */
    private fun updateHeaderUI(user: User) {
        binding.apply {
            // Bean count (số dư khả dụng)
            headerBeanCount.text = user.totalPoints.toString()

            // Rank name + emoji
            val (rankDisplayName, rankEmoji) = when (user.rank) {
                User.RANK_SILVER  -> Pair("Silver",  "🥈")
                User.RANK_GOLD    -> Pair("Gold",    "🥇")
                User.RANK_DIAMOND -> Pair("Diamond", "💎")
                else              -> Pair("Normal",  "☕")
            }
            headerRankName.text = "$rankEmoji $rankDisplayName"
            headerRankEmoji.text = rankEmoji

            // Gradient background theo hạng
            val bgRes = when (user.rank) {
                User.RANK_SILVER  -> R.drawable.bg_membership_card_silver
                User.RANK_GOLD    -> R.drawable.bg_membership_card_gold
                User.RANK_DIAMOND -> R.drawable.bg_membership_card_diamond
                else              -> R.drawable.bg_membership_card_normal
            }
            headerLayout.setBackgroundResource(bgRes)

            // Progress bar dựa trên lifetimePoints
            if (user.rank == User.RANK_DIAMOND) {
                progressSection.visibility = View.GONE
                diamondMessage.visibility  = View.VISIBLE
                rankProgressBar.progress  = 100
            } else {
                progressSection.visibility = View.VISIBLE
                diamondMessage.visibility  = View.GONE

                val minPoints  = User.getCurrentRankMinPoints(user.rank)
                val maxPoints  = User.getNextRankThreshold(user.rank)
                val range      = maxPoints - minPoints
                val current    = user.lifetimePoints - minPoints
                val progress   = if (range > 0) ((current * 100) / range).toInt() else 0
                val remaining  = (maxPoints - user.lifetimePoints).coerceAtLeast(0)
                val nextRank   = when (user.rank) {
                    User.RANK_NORMAL -> "Silver"
                    User.RANK_SILVER -> "Gold"
                    else             -> "Diamond"
                }

                rankProgressBar.progress = progress.coerceIn(0, 100)
                progressLabel.text = "Còn $remaining BEAN nữa để lên $nextRank"
                progressPercent.text = "$progress%"
            }
        }
    }
}
