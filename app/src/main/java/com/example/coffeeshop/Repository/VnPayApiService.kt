package com.example.coffeeshop.Repository

import com.example.coffeeshop.Model.PaymentRequest
import com.example.coffeeshop.Model.PaymentUrlResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface Retrofit để gọi API Backend (C# ASP.NET Core).
 * Endpoint chính: tạo URL thanh toán VNPAY.
 */
interface VnPayApiService {

    /**
     * Gửi yêu cầu tạo URL thanh toán VNPAY lên server.
     *
     * @param request Chứa orderId và amount
     * @return PaymentUrlResponse chứa URL thanh toán VNPAY
     */
    @POST("api/payment/create-payment-url")
    suspend fun createPaymentUrl(@Body request: PaymentRequest): PaymentUrlResponse

    companion object {
        /**
         * Base URL cho Backend API.
         *
         * LƯU Ý:
         * - Máy ảo Android (Emulator): dùng 10.0.2.2 thay cho localhost
         * - Thiết bị thật: thay bằng IP local của máy tính (VD: 192.168.1.xxx)
         * - Port 5000 là port mặc định của ASP.NET Core (HTTP)
         */
        private const val BASE_URL = "http://192.168.18.2:5282/"

        /**
         * Tạo instance Retrofit (Singleton-like pattern).
         * Sử dụng Gson converter để parse JSON response.
         */
        fun create(): VnPayApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(VnPayApiService::class.java)
        }
    }
}
