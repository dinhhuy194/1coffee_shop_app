package com.example.coffeeshop.Api

import com.example.coffeeshop.Model.MapboxGeocodeResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface gọi API Reverse Geocoding của Mapbox (v6).
 *
 * Chức năng: Chuyển đổi tọa độ (latitude, longitude) thành chuỗi địa chỉ văn bản
 * (Reverse Geocoding) để hiển thị cho người dùng khi chọn vị trí trên bản đồ.
 *
 * API Docs: https://docs.mapbox.com/api/search/geocoding-v6/
 */
interface MapboxApiService {

    /**
     * Gọi Reverse Geocoding: tọa độ → địa chỉ text.
     *
     * @param longitude   Kinh độ (VD: 106.6297)
     * @param latitude    Vĩ độ (VD: 10.8231)
     * @param accessToken Mapbox public access token (pk.xxx)
     * @return MapboxGeocodeResponse chứa danh sách features, lấy features[0].properties.full_address
     */
    @GET("search/geocode/v6/reverse")
    suspend fun reverseGeocode(
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("access_token") accessToken: String
    ): MapboxGeocodeResponse

    /**
     * Forward Geocoding: text địa chỉ → tọa độ + danh sách gợi ý.
     * Dùng cho search bar autocomplete trong MapboxPickerActivity và CheckoutActivity.
     *
     * @param query       Chuỗi địa chỉ người dùng nhập (VD: "123 Nguyễn Huệ")
     * @param accessToken Mapbox public access token (pk.xxx)
     * @param limit       Số kết quả tối đa trả về (mặc định 5)
     * @return MapboxGeocodeResponse chứa danh sách features (địa chỉ gợi ý)
     */
    @GET("search/geocode/v6/forward")
    suspend fun forwardGeocode(
        @Query("q") query: String,
        @Query("access_token") accessToken: String,
        @Query("limit") limit: Int = 5,
        @Query("language") language: String = "vi"
    ): MapboxGeocodeResponse

    companion object {
        /** Base URL cho Mapbox Geocoding API */
        private const val BASE_URL = "https://api.mapbox.com/"

        /**
         * Tạo instance Retrofit cho Mapbox API.
         * Sử dụng Gson converter để parse JSON response.
         *
         * Pattern tương tự VnPayApiService.create()
         */
        fun create(): MapboxApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(MapboxApiService::class.java)
        }
    }
}
