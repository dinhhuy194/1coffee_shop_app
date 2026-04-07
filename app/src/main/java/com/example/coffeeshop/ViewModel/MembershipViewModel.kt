package com.example.coffeeshop.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Model.BeanVoucher
import com.example.coffeeshop.Model.PointHistory
import com.example.coffeeshop.Model.Privilege
import com.example.coffeeshop.Model.User
import com.example.coffeeshop.Repository.UserRepository
import kotlinx.coroutines.launch

class MembershipViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _privileges = MutableLiveData<List<Privilege>>()
    val privileges: LiveData<List<Privilege>> = _privileges

    private val _vouchers = MutableLiveData<List<BeanVoucher>>()
    val vouchers: LiveData<List<BeanVoucher>> = _vouchers

    private val _pointHistory = MutableLiveData<List<PointHistory>>()
    val pointHistory: LiveData<List<PointHistory>> = _pointHistory

    private val _redeemState = MutableLiveData<RedeemState>(RedeemState.Idle)
    val redeemState: LiveData<RedeemState> = _redeemState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Load toàn bộ dữ liệu membership: profile, privileges, vouchers, history
     */
    fun loadMembershipData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Load user
            userRepository.getUserProfile(userId).onSuccess { user ->
                _userProfile.value = user
            }

            // Load history
            loadPointHistory(userId)

            _isLoading.value = false
        }

        // Load danh sách tĩnh
        _privileges.value = buildPrivilegeList()
        _vouchers.value = buildVoucherList()
    }

    /**
     * Load lịch sử điểm
     */
    private suspend fun loadPointHistory(userId: String) {
        userRepository.getPointHistory(userId).onSuccess { history ->
            _pointHistory.value = history
        }.onFailure {
            _pointHistory.value = emptyList()
        }
    }

    /**
     * Đổi voucher bằng BEAN
     */
    fun redeemVoucher(userId: String, voucher: BeanVoucher) {
        viewModelScope.launch {
            _redeemState.value = RedeemState.Loading

            val currentPoints = _userProfile.value?.totalPoints ?: 0L
            if (currentPoints < voucher.beanCost) {
                _redeemState.value = RedeemState.Error("Bạn không đủ BEAN để đổi voucher này")
                return@launch
            }

            userRepository.redeemVoucher(userId, voucher.beanCost, voucher.name, voucher)
                .onSuccess { remainingPoints ->
                    // Cập nhật local state
                    _userProfile.value = _userProfile.value?.copy(totalPoints = remainingPoints)
                    _redeemState.value = RedeemState.Success("Đổi \"${voucher.name}\" thành công! 🎉")
                    // Refresh history
                    loadPointHistory(userId)
                }
                .onFailure { e ->
                    _redeemState.value = RedeemState.Error(e.message ?: "Đổi voucher thất bại")
                }
        }
    }

    fun resetRedeemState() {
        _redeemState.value = RedeemState.Idle
    }

    // ─── Static Data Builders ────────────────────────────────────────────────

    /**
     * Danh sách đặc quyền theo từng hạng.
     * requiredRank xác định hạng cần đạt để mở khoá đặc quyền này.
     */
    private fun buildPrivilegeList(): List<Privilege> = listOf(
        // Normal
        Privilege(
            title = "Tích BEAN mỗi đơn hàng",
            description = "Nhận 2 BEAN cho mỗi $1 chi tiêu",
            iconEmoji = "🫘",
            requiredRank = User.RANK_NORMAL
        ),
        Privilege(
            title = "Ưu đãi sinh nhật",
            description = "Giảm 20% vào tháng sinh nhật",
            iconEmoji = "🎂",
            requiredRank = User.RANK_NORMAL
        ),
        // Silver
        Privilege(
            title = "Tích BEAN nâng cao",
            description = "Nhận 3 BEAN cho mỗi $1 chi tiêu",
            iconEmoji = "⭐",
            requiredRank = User.RANK_SILVER
        ),
        Privilege(
            title = "Miễn phí size nâng cấp",
            description = "Nâng size miễn phí 2 lần/tháng",
            iconEmoji = "🥤",
            requiredRank = User.RANK_SILVER
        ),
        Privilege(
            title = "Ưu tiên đặt hàng",
            description = "Hàng đợi ưu tiên trong giờ cao điểm",
            iconEmoji = "⚡",
            requiredRank = User.RANK_SILVER
        ),
        // Gold
        Privilege(
            title = "Tích BEAN vàng",
            description = "Nhận 4 BEAN cho mỗi $1 chi tiêu",
            iconEmoji = "🥇",
            requiredRank = User.RANK_GOLD
        ),
        Privilege(
            title = "Miễn phí 1 ly/tháng",
            description = "Tặng 1 ly size M miễn phí mỗi tháng",
            iconEmoji = "☕",
            requiredRank = User.RANK_GOLD
        ),
        Privilege(
            title = "Đặc sản mùa hàng sớm",
            description = "Truy cập menu mới trước 24 giờ",
            iconEmoji = "🌟",
            requiredRank = User.RANK_GOLD
        ),
        // Diamond
        Privilege(
            title = "Tích BEAN kim cương",
            description = "Nhận 5 BEAN cho mỗi $1 chi tiêu",
            iconEmoji = "💎",
            requiredRank = User.RANK_DIAMOND
        ),
        Privilege(
            title = "Concierge cà phê riêng",
            description = "Tư vấn pha chế cá nhân hoá theo khẩu vị",
            iconEmoji = "👑",
            requiredRank = User.RANK_DIAMOND
        ),
        Privilege(
            title = "Miễn phí ship toàn bộ",
            description = "Giao hàng miễn phí không giới hạn",
            iconEmoji = "🚀",
            requiredRank = User.RANK_DIAMOND
        ),
        Privilege(
            title = "Sự kiện VIP hàng quý",
            description = "Mời tham dự sự kiện thử nghiệm sản phẩm mới",
            iconEmoji = "🎪",
            requiredRank = User.RANK_DIAMOND
        )
    )

    /**
     * Danh sách voucher có thể đổi bằng BEAN
     */
    private fun buildVoucherList(): List<BeanVoucher> = listOf(
        BeanVoucher(
            id = "v001",
            name = "Giảm 10.000đ",
            description = "Áp dụng cho đơn hàng từ 50.000đ",
            beanCost = 100L,
            discountValue = 10_000L,
            iconEmoji = "🎫"
        ),
        BeanVoucher(
            id = "v002",
            name = "Giảm 25.000đ",
            description = "Áp dụng cho đơn hàng từ 80.000đ",
            beanCost = 250L,
            discountValue = 25_000L,
            iconEmoji = "🎟️"
        ),
        BeanVoucher(
            id = "v003",
            name = "Miễn phí Americano",
            description = "Đổi 1 ly Americano size M miễn phí",
            beanCost = 350L,
            discountValue = 45_000L,
            iconEmoji = "☕"
        ),
        BeanVoucher(
            id = "v004",
            name = "Giảm 50.000đ",
            description = "Áp dụng cho đơn hàng từ 150.000đ",
            beanCost = 500L,
            discountValue = 50_000L,
            iconEmoji = "💰"
        ),
        BeanVoucher(
            id = "v005",
            name = "Miễn phí Cake Slice",
            description = "1 miếng bánh tuỳ chọn",
            beanCost = 600L,
            discountValue = 65_000L,
            iconEmoji = "🍰"
        ),
        BeanVoucher(
            id = "v006",
            name = "Giảm 100.000đ",
            description = "Áp dụng cho đơn hàng từ 300.000đ",
            beanCost = 1000L,
            discountValue = 100_000L,
            iconEmoji = "🎁"
        )
    )

    // ─── States ──────────────────────────────────────────────────────────────
    sealed class RedeemState {
        object Idle : RedeemState()
        object Loading : RedeemState()
        data class Success(val message: String) : RedeemState()
        data class Error(val message: String) : RedeemState()
    }
}
