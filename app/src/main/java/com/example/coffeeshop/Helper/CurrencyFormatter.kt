package com.example.coffeeshop.Helper

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * CurrencyFormatter — Tiện ích format giá tiền VND thống nhất toàn app.
 *
 * Sử dụng:
 *   CurrencyFormatter.format(45000.0)  → "45.000₫"
 *   CurrencyFormatter.format(120000.0) → "120.000₫"
 */
object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,##0", symbols)

    /**
     * Format số tiền sang chuỗi VND.
     * @param amount Số tiền (Double)
     * @return Chuỗi đã format, ví dụ "45.000₫"
     */
    fun format(amount: Double): String {
        return "${formatter.format(amount)}₫"
    }

    /**
     * Format số tiền với dấu trừ (dùng cho discount).
     * @param amount Số tiền (Double)
     * @return Chuỗi đã format, ví dụ "-10.000₫"
     */
    fun formatDiscount(amount: Double): String {
        return "-${formatter.format(amount)}₫"
    }
}
