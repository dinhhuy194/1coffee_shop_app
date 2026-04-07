package com.example.coffeeshop.Model

/**
 * Model response cho Mapbox Geocoding API v6 (Reverse Geocoding).
 *
 * API Endpoint: GET https://api.mapbox.com/search/geocode/v6/reverse
 * Tài liệu: https://docs.mapbox.com/api/search/geocoding-v6/
 *
 * Cấu trúc JSON trả về (rút gọn):
 * {
 *   "type": "FeatureCollection",
 *   "features": [
 *     {
 *       "type": "Feature",
 *       "properties": {
 *         "full_address": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP HCM",
 *         "name": "123 Nguyễn Huệ",
 *         "place_formatted": "Phường Bến Nghé, Quận 1, TP HCM"
 *       }
 *     }
 *   ]
 * }
 */

/**
 * Response gốc chứa danh sách Feature (thường lấy phần tử đầu tiên).
 */
data class MapboxGeocodeResponse(
    val features: List<MapboxFeature>?
)

/**
 * Mỗi Feature đại diện cho 1 kết quả geocoding.
 */
data class MapboxFeature(
    val properties: MapboxProperties?,
    val geometry: MapboxGeometry?      // Chứa tọa độ [lng, lat] của địa điểm
)

/**
 * Geometry chứa tọa độ điểm địa lý.
 * type = "Point", coordinates = [longitude, latitude]
 */
data class MapboxGeometry(
    val type: String?,
    val coordinates: List<Double>?     // [0] = longitude, [1] = latitude
)

/**
 * Properties chứa thông tin địa chỉ chi tiết.
 *
 * @property full_address    Địa chỉ đầy đủ (VD: "123 Nguyễn Huệ, Q1, TP HCM")
 * @property name            Tên ngắn của địa điểm (VD: "123 Nguyễn Huệ")
 * @property place_formatted Phần địa chỉ phụ (VD: "Phường Bến Nghé, Quận 1")
 */
data class MapboxProperties(
    val full_address: String?,
    val name: String?,
    val place_formatted: String?
)
