package com.example.coffeeshop.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.coffeeshop.Activity.LoginActivity
import com.example.coffeeshop.Activity.MembershipDetailActivity
import com.example.coffeeshop.Activity.MyVouchersActivity
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.ProfileViewModel
import com.example.coffeeshop.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()
        setupListeners()
        observeViewModel()

        viewModel.loadProfile()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupListeners() {
        binding.apply {
            backBtn.setOnClickListener {
                requireActivity().finish()
            }

            authBtn.setOnClickListener {
                viewModel.isLoggedIn.value?.let { isLoggedIn ->
                    if (isLoggedIn) {
                        viewModel.logout(googleSignInClient)
                    } else {
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                    }
                }
            }

            // ── Membership Card → MembershipDetailActivity ──────────────────
            membershipCard.setOnClickListener {
                if (viewModel.isLoggedIn.value == true) {
                    startActivity(Intent(requireContext(), MembershipDetailActivity::class.java))
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Vui lòng đăng nhập để xem thẻ thành viên",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // ── Menu items ───────────────────────────────────────────────────
            favoritesBtn.setOnClickListener {
                startActivity(
                    Intent(
                        requireContext(),
                        com.example.coffeeshop.Activity.FavoriteActivity::class.java
                    )
                )
            }

            myVouchersBtn.setOnClickListener {
                if (viewModel.isLoggedIn.value == true) {
                    startActivity(
                        Intent(requireContext(), MyVouchersActivity::class.java)
                    )
                } else {
                    Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                }
            }

            paymentBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Payment Methods - Coming soon", Toast.LENGTH_SHORT).show()
            }

            settingsBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Settings - Coming soon", Toast.LENGTH_SHORT).show()
            }

            languageBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Language - Coming soon", Toast.LENGTH_SHORT).show()
            }

            aboutBtn.setOnClickListener {
                Toast.makeText(requireContext(), "About Us - Coming soon", Toast.LENGTH_SHORT).show()
            }

            feedbackBtn.setOnClickListener {
                Toast.makeText(requireContext(), "Feedback - Coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            binding.authBtn.text = if (isLoggedIn) "Logout" else "Login"
            binding.membershipCard.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.apply {
                if (user != null) {
                    nameTxt.text  = user.name
                    emailTxt.text = user.email

                    user.photoUrl?.let { url ->
                        Glide.with(this@ProfileFragment)
                            .load(url)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(avatarImg)
                    }

                    updateMembershipCard(user)
                } else {
                    nameTxt.text  = "Guest User"
                    emailTxt.text = "Login to access more features"
                    avatarImg.setImageResource(android.R.drawable.sym_def_app_icon)
                    membershipCard.visibility = View.GONE
                }
            }
        }

        viewModel.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.LogoutState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ProfileViewModel.LogoutState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
                is ProfileViewModel.LogoutState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Logout error: ${state.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Cập nhật giao diện Membership Card theo thông tin user hiện tại.
     * - Đổi gradient background theo hạng
     * - Hiển thị số BEAN (totalPoints = số dư khả dụng)
     * - Progress bar dựa trên lifetimePoints (tổng BEAN tích lũy, không bị trừ)
     * - Hạng Diamond: ẩn progress, hiện "cấp tối cao"
     */
    private fun updateMembershipCard(user: User) {
        binding.apply {
            membershipBeanCount.text = user.totalPoints.toString()

            val (rankDisplayName, rankEmoji, bgRes) = when (user.rank) {
                User.RANK_SILVER  -> Triple("Silver",  "🥈", R.drawable.bg_membership_card_silver)
                User.RANK_GOLD    -> Triple("Gold",    "🥇", R.drawable.bg_membership_card_gold)
                User.RANK_DIAMOND -> Triple("Diamond", "💎", R.drawable.bg_membership_card_diamond)
                else              -> Triple("Normal",  "☕", R.drawable.bg_membership_card_normal)
            }
            membershipRankLabel.text = "$rankEmoji $rankDisplayName"
            membershipRankEmoji.text = rankEmoji
            membershipCard.getChildAt(0)?.setBackgroundResource(bgRes)

            if (user.rank == User.RANK_DIAMOND) {
                membershipProgressSection.visibility = View.GONE
                membershipDiamondMsg.visibility      = View.VISIBLE
                membershipProgressBar.progress       = 100
            } else {
                membershipProgressSection.visibility = View.VISIBLE
                membershipDiamondMsg.visibility      = View.GONE

                // Progress bar dựa trên lifetimePoints (tổng tích lũy)
                val minPoints = User.getCurrentRankMinPoints(user.rank)
                val maxPoints = User.getNextRankThreshold(user.rank)
                val range     = maxPoints - minPoints
                val current   = user.lifetimePoints - minPoints
                val progress  = if (range > 0) ((current * 100) / range).toInt() else 0
                val remaining = (maxPoints - user.lifetimePoints).coerceAtLeast(0)
                val nextRank  = when (user.rank) {
                    User.RANK_NORMAL -> "Silver"
                    User.RANK_SILVER -> "Gold"
                    else             -> "Diamond"
                }

                membershipProgressBar.progress = progress.coerceIn(0, 100)
                membershipProgressLabel.text   = "Còn $remaining BEAN nữa lên $nextRank"
                membershipProgressPercent.text = "$progress%"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
