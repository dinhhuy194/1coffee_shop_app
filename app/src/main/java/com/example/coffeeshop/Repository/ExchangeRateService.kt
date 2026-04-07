package com.example.coffeeshop.Repository

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Model response từ API tỷ giá (exchangerate-api.com).
 * API trả về JSON chứa các tỷ giá quy đổi từ đồng tiền gốc (USD).
 *
 * Ví dụ response:
 * {
 *   "result": "success",
 *   "base_code": "USD",
 *   "rates": {
 *     "VND": 25410.0,
 *     "EUR": 0.92,
 *     ...
 *   }
 * }
 */
data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String,

    @SerializedName("rates")
    val rates: Map<String, Double>
)

/**
 * Service Retrofit để lấy tỷ giá quy đổi USD → VND theo thời gian thực.
 *
 * Sử dụng API miễn phí: https://open.er-api.com
 * - Không cần API key
 * - Giới hạn: 1,500 requests/tháng (đủ cho app nhỏ)
 * - Dữ liệu cập nhật mỗi 24 giờ
 */
interface ExchangeRateService {

    /**
     * Lấy tỷ giá mới nhất từ USD sang tất cả các đồng tiền.
     *
     * @return ExchangeRateResponse chứa map tỷ giá {currencyCode -> rate}
     */
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateResponse

    companion object {
        /** URL gốc của API tỷ giá miễn phí */
        private const val BASE_URL = "https://open.er-api.com/"

        /** Tỷ giá mặc định USD → VND (dùng khi API lỗi) */
        const val DEFAULT_USD_TO_VND = 25_000.0

        /** Cache tỷ giá để tránh gọi API quá nhiều lần */
        private var cachedRate: Double? = null
        private var cacheTimestamp: Long = 0

        /** Thời gian cache hợp lệ: 1 giờ (milliseconds) */
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L

        /**
         * Tạo Retrofit instance cho API tỷ giá.
         */
        fun create(): ExchangeRateService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExchangeRateService::class.java)
        }

        /**
         * Lấy tỷ giá USD → VND (có cache 1 giờ).
         * Nếu cache còn hợp lệ → trả về ngay, không gọi API.
         * Nếu cache hết hạn hoặc API lỗi → dùng tỷ giá mặc định.
         *
         * @return Tỷ giá USD → VND
         */
        suspend fun getUsdToVndRate(): Double {
            // Kiểm tra cache còn hợp lệ không
            val now = System.currentTimeMillis()
            cachedRate?.let { rate ->
                if (now - cacheTimestamp < CACHE_DURATION_MS) {
                    return rate
                }
            }

            // Cache hết hạn → gọi API lấy tỷ giá mới
            return try {
                val service = create()
                val response = service.getLatestRates()

                if (response.result == "success") {
                    val vndRate = response.rates["VND"] ?: DEFAULT_USD_TO_VND

                    // Lưu cache
                    cachedRate = vndRate
                    cacheTimestamp = now

                    vndRate
                } else {
                    cachedRate ?: DEFAULT_USD_TO_VND
                }
            } catch (e: Exception) {
                // Lỗi mạng hoặc API → dùng cache cũ hoặc tỷ giá mặc định
                cachedRate ?: DEFAULT_USD_TO_VND
            }
        }
    }
}
