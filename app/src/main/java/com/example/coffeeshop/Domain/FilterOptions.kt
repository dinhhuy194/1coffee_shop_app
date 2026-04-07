package com.example.coffeeshop.Domain

/**
 * FilterOptions – Data class lưu trạng thái bộ lọc sản phẩm.
 *
 * SortBy   : cách sắp xếp
 * categoryId: lọc theo loại ("" = tất cả)
 * minPrice / maxPrice: khoảng giá (0 = không giới hạn)
 */
data class FilterOptions(
    val sortBy: SortBy = SortBy.DEFAULT,
    val categoryId: String = "",           // "" = All
    val minPrice: Double = 0.0,
    val maxPrice: Double = Double.MAX_VALUE
) {
    enum class SortBy {
        DEFAULT,        // Mặc định (thứ tự từ Firebase)
        PRICE_ASC,      // Giá thấp → cao
        PRICE_DESC,     // Giá cao → thấp
        RATING_DESC     // Rating cao nhất
    }

    /** True nếu không có filter nào được apply */
    val isDefault: Boolean
        get() = sortBy == SortBy.DEFAULT
                && categoryId.isEmpty()
                && minPrice == 0.0
                && maxPrice == Double.MAX_VALUE
}
