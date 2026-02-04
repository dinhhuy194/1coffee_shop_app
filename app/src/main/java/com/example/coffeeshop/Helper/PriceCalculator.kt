package com.example.coffeeshop.Helper

object PriceCalculator {
    private val SIZE_PRICES = mapOf(
        "Small" to 0.0,
        "Medium" to 1.0,
        "Large" to 2.0
    )
    
    private val ICE_PRICES = mapOf(
        "Nóng" to 0.0,
        "Đá chung" to 0.0,
        "Đá riêng" to 0.0
    )
    
    private val SUGAR_PRICES = mapOf(
        "Không" to 0.0,
        "Ít" to 0.0,
        "Bình thường" to 0.0
    )
    
    /**
     * Tính tổng giá dựa trên các tùy chọn
     * @param basePrice Giá gốc của sản phẩm
     * @param size Size được chọn (Small/Medium/Large)
     * @param iceOption Tùy chọn đá (Nóng/Đá chung/Đá riêng)
     * @param sugarOption Tùy chọn đường (Không/Ít/Bình thường)
     * @param quantity Số lượng
     * @return Tổng giá
     */
    fun calculateTotalPrice(
        basePrice: Double,
        size: String,
        iceOption: String,
        sugarOption: String,
        quantity: Int
    ): Double {
        val sizePrice = SIZE_PRICES[size] ?: 1.0  // Default Medium
        val icePrice = ICE_PRICES[iceOption] ?: 0.0
        val sugarPrice = SUGAR_PRICES[sugarOption] ?: 0.0
        
        val unitPrice = basePrice + sizePrice + icePrice + sugarPrice
        return unitPrice * quantity
    }
    
    /**
     * Lấy giá bổ sung cho size
     */
    fun getSizePrice(size: String): Double {
        return SIZE_PRICES[size] ?: 1.0
    }
    
    /**
     * Lấy giá bổ sung cho ice option
     */
    fun getIcePrice(iceOption: String): Double {
        return ICE_PRICES[iceOption] ?: 0.0
    }
}
