package com.example.coffeeshop.Helper

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coffeeshop.Activity.CartActivity
import com.example.coffeeshop.Activity.FavoriteActivity
import com.example.coffeeshop.Activity.MainActivity
import com.example.coffeeshop.Activity.OrderHistoryActivity
import com.example.coffeeshop.Activity.ProfileActivity
import com.example.coffeeshop.R

/**
 * BottomNavHelper – Setup click listeners cho BottomNavBar được share qua các Activity.
 *
 * Sử dụng:
 *   BottomNavHelper.setup(this, BottomNavHelper.Tab.CART)
 *
 * Convention: highlight tab hiện tại bằng alpha 1.0f, các tab khác 0.55f.
 */
object BottomNavHelper {

    enum class Tab { EXPLORER, CART, FAVOURITE, ORDER, PROFILE }

    /**
     * @param activity  Activity đang hiển thị
     * @param activeTab Tab đang active (để highlight)
     * @param rootView  Root view chứa BottomNavBar (null = dùng activity.window.decorView)
     */
    fun setup(activity: Activity, activeTab: Tab, rootView: View? = null) {
        val root = rootView ?: activity.window.decorView

        val navExplorer  = root.findViewById<View>(R.id.navExplorer)
        val navCart      = root.findViewById<View>(R.id.navCart)
        val navFavourite = root.findViewById<View>(R.id.navFavourite)
        val navOrder     = root.findViewById<View>(R.id.navOrder)
        val navProfile   = root.findViewById<View>(R.id.navProfile)

        // Nếu views không tồn tại trong layout này thì return (an toàn)
        if (navExplorer == null) return

        // Highlight tab hiện tại
        val tabs = listOf(navExplorer, navCart, navFavourite, navOrder, navProfile)
        val activeIndex = activeTab.ordinal
        tabs.forEachIndexed { index, view ->
            view.alpha = if (index == activeIndex) 1.0f else 0.55f
        }

        // Tự động thêm padding bottom = chiều cao system navigation bar
        // để tránh bị gesture bar / button navbar che mất bottom nav của app
        val bottomNavContainer = root.findViewById<View>(R.id.bottomNav)
        if (bottomNavContainer != null) {
            applySystemNavInsets(bottomNavContainer)
        }

        // Click listeners – dùng FLAG_ACTIVITY_REORDER_TO_FRONT để tránh stack chồng chất
        fun navigate(clazz: Class<*>) {
            if (activity.javaClass != clazz) {
                val intent = Intent(activity, clazz).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(intent)
            }
        }

        navExplorer.setOnClickListener  { navigate(MainActivity::class.java) }
        navCart.setOnClickListener      { navigate(CartActivity::class.java) }
        navFavourite.setOnClickListener { navigate(FavoriteActivity::class.java) }
        navOrder.setOnClickListener     { navigate(OrderHistoryActivity::class.java) }
        navProfile.setOnClickListener   { navigate(ProfileActivity::class.java) }
    }

    /**
     * Lắng nghe WindowInsets và tự động thêm paddingBottom vào [view]
     * bằng đúng chiều cao của system navigation bar (gesture bar / button navbar).
     *
     * Cách hoạt động:
     * - Trên điện thoại có gesture navigation bar (Android 10+): thêm padding ~34–48dp
     * - Trên điện thoại có 3-button navbar: thêm padding ~42–56dp
     * - Trên điện thoại không có system navbar (hoặc đã tắt): padding = 0
     *
     * Hàm này lưu lại paddingTop/Start/End gốc để không bị reset sau mỗi lần insets thay đổi.
     */
    private fun applySystemNavInsets(view: View) {
        // Lưu padding gốc để không bị cộng dồn nhiều lần
        val originalPaddingBottom = view.paddingBottom
        val originalPaddingTop    = view.paddingTop
        val originalPaddingLeft   = view.paddingLeft
        val originalPaddingRight  = view.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(
                originalPaddingLeft,
                originalPaddingTop,
                originalPaddingRight,
                originalPaddingBottom + navBarInsets.bottom
            )
            windowInsets
        }
    }
}
