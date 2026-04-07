package com.example.coffeeshop.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coffeeshop.Api.MapboxApiService
import com.example.coffeeshop.Model.MapboxFeature
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ActivityMapboxPickerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity chọn địa chỉ giao hàng bằng bản đồ Mapbox SDK v11.
 *
 * Tính năng:
 * 1. Bản đồ full screen với marker ghim cố định tại tâm.
 * 2. Tự động di chuyển đến vị trí người dùng khi mở màn hình.
 * 3. Search bar ở đầu trang: nhập địa chỉ → gợi ý → bấm chọn → camera bay đến vị trí.
 * 4. Kéo bản đồ → camera idle → Reverse Geocoding → hiện địa chỉ tạm ở CardView dưới.
 * 5. Bấm "Xác nhận" → lưu địa chỉ Firestore → RESULT_OK → finish().
 */
class MapboxPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapboxPickerBinding

    /** Mapbox public token (inject bởi build.gradle resValue từ gradle.properties) */
    private lateinit var mapboxToken: String

    /** Retrofit service cho Mapbox Geocoding API */
    private val mapboxApiService: MapboxApiService by lazy { MapboxApiService.create() }

    private val db = FirebaseFirestore.getInstance()

    /** Địa chỉ tạm thời từ Reverse Geocoding để xác nhận */
    private var tempAddress: String = ""

    /** Tọa độ tâm camera hiện tại */
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    /** Job debounce — tránh spam API khi người dùng kéo nhanh */
    private var geocodeJob: Job? = null

    /** Job debounce — tránh spam search API khi người dùng gõ nhanh */
    private var searchJob: Job? = null

    /** Danh sách kết quả Forward Geocoding (cho suggestions) */
    private var searchResults: List<MapboxFeature> = emptyList()

    /** Adapter cho ListView suggestions */
    private lateinit var suggestionsAdapter: ArrayAdapter<String>

    /**
     * Launcher xin quyền vị trí.
     * Cấp → bật user location puck + auto-center camera.
     * Từ chối → bản đồ vẫn hoạt động, mặc định TP.HCM.
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            Log.d(TAG, "✅ Quyền vị trí được cấp")
            enableUserLocationAndCenter()
        } else {
            Log.w(TAG, "⚠️ Quyền vị trí bị từ chối")
            Toast.makeText(this, "Không có quyền vị trí. Kéo bản đồ để chọn địa chỉ.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapboxPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapboxToken = getString(R.string.mapbox_access_token)

        if (mapboxToken.isBlank()) {
            Log.e(TAG, "❌ MAPBOX_ACCESS_TOKEN chưa cấu hình!")
            Toast.makeText(this, "Bản đồ chưa được cấu hình.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Khởi tạo adapter cho suggestions list
        suggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.suggestionsList.adapter = suggestionsAdapter

        setupMap()
        setupSearchBar()
        setupListeners()
    }

    // ─────────────────────────────────────────────────
    //  MAP SETUP
    // ─────────────────────────────────────────────────

    /**
     * Khởi tạo bản đồ Mapbox v11:
     * 1. Load style Streets
     * 2. Mặc định camera về TP.HCM
     * 3. Xin quyền vị trí → auto-center nếu được cấp
     * 4. Đăng ký camera idle listener (reverse geocode khi dừng kéo)
     */
    private fun setupMap() {
        binding.mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { _ ->
            Log.d(TAG, "✅ Mapbox style loaded")

            // Vị trí mặc định: Trung tâm TP. HCM
            binding.mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(106.7009, 10.7769))
                    .zoom(14.0)
                    .build()
            )

            // Xin quyền vị trí rồi auto-center về user location
            requestLocationPermission()

            // Bắt đầu lắng nghe camera idle để reverse geocode
            subscribeCameraIdleListener()
        }
    }

    /**
     * Lắng nghe sự kiện camera dừng (idle).
     * Debounce 600ms → lấy tọa độ tâm → gọi Reverse Geocoding.
     */
    private fun subscribeCameraIdleListener() {
        binding.mapView.mapboxMap.subscribeMapIdle {
            // Tọa độ tâm màn hình (chỗ ghim cố định)
            val center = ScreenCoordinate(
                binding.mapView.width / 2.0,
                binding.mapView.height / 2.0
            )
            val coord = binding.mapView.mapboxMap.coordinateForPixel(center)
            currentLat = coord.latitude()
            currentLng = coord.longitude()

            Log.d(TAG, "📍 Camera idle: lat=$currentLat, lng=$currentLng")

            geocodeJob?.cancel()
            geocodeJob = lifecycleScope.launch {
                delay(600L)
                reverseGeocode(currentLat, currentLng)
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  SEARCH BAR SETUP
    // ─────────────────────────────────────────────────

    /**
     * Thiết lập search bar: TextWatcher debounce → forwardGeocode → cập nhật suggestions.
     */
    private fun setupSearchBar() {
        // TextWatcher: mỗi lần gõ, debounce 500ms rồi tìm kiếm
        binding.searchAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                // Nút xóa text
                binding.clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.length < 3) {
                    // Ẩn suggestions nếu query quá ngắn
                    hideSuggestions()
                    return
                }

                // Debounce: hủy job cũ, đợi 500ms rồi search
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500L)
                    forwardGeocode(query)
                }
            }
        })

        // Bấm Search trên bàn phím → tìm kiếm ngay
        binding.searchAddressEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchAddressEditText.text.toString().trim()
                if (query.isNotBlank()) {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch { forwardGeocode(query) }
                }
                true
            } else false
        }

        // Nút xóa search text
        binding.clearSearchBtn.setOnClickListener {
            binding.searchAddressEditText.setText("")
            hideSuggestions()
        }

        // Chọn 1 gợi ý từ danh sách → camera bay đến vị trí đó
        binding.suggestionsList.setOnItemClickListener { _, _, position, _ ->
            val selected = searchResults.getOrNull(position) ?: return@setOnItemClickListener
            val coords = selected.geometry?.coordinates
            val address = selected.properties?.full_address
                ?: selected.properties?.name
                ?: "Địa chỉ không xác định"

            if (coords != null && coords.size >= 2) {
                val lng = coords[0]
                val lat = coords[1]

                // Bay camera đến vị trí được chọn
                flyToLocation(lat, lng)

                // Cập nhật địa chỉ tạm ngay lập tức (không cần chờ idle)
                tempAddress = address
                binding.tempAddressTxt.text = address
                binding.tempAddressTxt.setTextColor(getColor(android.R.color.black))
                binding.confirmAddressBtn.isEnabled = true

                Log.d(TAG, "🔍 Chọn từ gợi ý: $address | lat=$lat, lng=$lng")
            }

            // Ẩn suggestions + đóng bàn phím
            hideSuggestions()
            binding.searchAddressEditText.setText(address)
            hideKeyboard()
        }
    }

    /**
     * Gọi Mapbox Forward Geocoding API (text → danh sách địa chỉ gợi ý).
     * Hiển thị kết quả vào suggestions ListView.
     */
    private suspend fun forwardGeocode(query: String) {
        Log.d(TAG, "🔍 Forward geocoding: $query")
        try {
            val response = mapboxApiService.forwardGeocode(
                query = query,
                accessToken = mapboxToken,
                limit = 5
            )

            val features = response.features
            if (!features.isNullOrEmpty()) {
                searchResults = features

                // Hiển thị text address trong adapter
                val labels = features.map { feature ->
                    feature.properties?.full_address
                        ?: feature.properties?.name
                        ?: "Địa chỉ không xác định"
                }

                runOnUiThread {
                    suggestionsAdapter.clear()
                    suggestionsAdapter.addAll(labels)
                    suggestionsAdapter.notifyDataSetChanged()
                    binding.suggestionsCard.visibility = View.VISIBLE
                }
            } else {
                runOnUiThread { hideSuggestions() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi forward geocoding: ${e.message}")
            runOnUiThread { hideSuggestions() }
        }
    }

    // ─────────────────────────────────────────────────
    //  REVERSE GEOCODING
    // ─────────────────────────────────────────────────

    /**
     * Gọi Mapbox Reverse Geocoding API (tọa độ → địa chỉ text).
     * Cập nhật tempAddressTxt và enable nút xác nhận.
     */
    private suspend fun reverseGeocode(lat: Double, lng: Double) {
        binding.geocodeProgressBar.visibility = View.VISIBLE
        binding.confirmAddressBtn.isEnabled = false
        binding.tempAddressTxt.text = "Đang tìm địa chỉ..."

        try {
            val response = mapboxApiService.reverseGeocode(
                longitude = lng,
                latitude = lat,
                accessToken = mapboxToken
            )

            val address = response.features?.firstOrNull()?.properties?.full_address

            if (!address.isNullOrBlank()) {
                tempAddress = address
                binding.tempAddressTxt.text = address
                binding.tempAddressTxt.setTextColor(getColor(android.R.color.black))
                binding.confirmAddressBtn.isEnabled = true
                Log.d(TAG, "✅ Reverse geocode: $address")
            } else {
                tempAddress = ""
                binding.tempAddressTxt.text = "Không tìm thấy địa chỉ. Vui lòng di chuyển bản đồ."
                binding.tempAddressTxt.setTextColor(getColor(R.color.grey))
                binding.confirmAddressBtn.isEnabled = false
            }
        } catch (e: Exception) {
            tempAddress = ""
            binding.tempAddressTxt.text = "Lỗi tải địa chỉ. Kiểm tra kết nối mạng."
            binding.tempAddressTxt.setTextColor(getColor(R.color.grey))
            binding.confirmAddressBtn.isEnabled = false
            Log.e(TAG, "❌ Lỗi reverse geocoding: ${e.message}")
        } finally {
            binding.geocodeProgressBar.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────────
    //  BUTTONS & LISTENERS
    // ─────────────────────────────────────────────────

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.myLocationBtn.setOnClickListener {
            moveToUserLocation()
        }

        binding.confirmAddressBtn.setOnClickListener {
            if (tempAddress.isBlank()) {
                Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAddressToFirestore(tempAddress)
        }
    }

    // ─────────────────────────────────────────────────
    //  LOCATION PERMISSION & USER LOCATION
    // ─────────────────────────────────────────────────

    private fun requestLocationPermission() {
        val fineOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineOk || coarseOk) {
            enableUserLocationAndCenter()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /**
     * Bật user location puck và tự động di chuyển camera đến vị trí người dùng.
     * Đây là tính năng auto-center khi mở màn hình.
     */
    @SuppressLint("MissingPermission")
    private fun enableUserLocationAndCenter() {
        try {
            binding.mapView.location.apply {
                enabled = true
                locationPuck = createDefault2DPuck(withBearing = false)
            }
            // Auto-center về vị trí người dùng ngay khi mở màn hình
            moveToUserLocation()
            Log.d(TAG, "✅ User location enabled, auto-centering")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi bật user location: ${e.message}")
        }
    }

    private fun moveToUserLocation() {
        try {
            binding.mapView.viewport.transitionTo(
                binding.mapView.viewport.makeFollowPuckViewportState(),
                binding.mapView.viewport.makeImmediateViewportTransition()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi di chuyển đến vị trí hiện tại: ${e.message}")
            Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────
    //  CAMERA ANIMATION
    // ─────────────────────────────────────────────────

    /**
     * Bay camera đến tọa độ được chọn (từ search suggestions).
     * Dùng flyTo animation cho trải nghiệm mượt mà.
     *
     * @param lat Vĩ độ đích
     * @param lng Kinh độ đích
     */
    private fun flyToLocation(lat: Double, lng: Double) {
        binding.mapView.mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(16.0)
                .build(),
            MapAnimationOptions.Builder()
                .duration(1200L)
                .build()
        )
    }

    // ─────────────────────────────────────────────────
    //  FIRESTORE SAVE
    // ─────────────────────────────────────────────────

    /**
     * Lưu địa chỉ vào Firestore users/{userId}.address.
     * Sau khi lưu thành công → RESULT_OK → finish().
     */
    private fun saveAddressToFirestore(address: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu địa chỉ", Toast.LENGTH_SHORT).show()
            return
        }

        binding.confirmAddressBtn.isEnabled = false
        binding.geocodeProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("address", address)
                    .await()

                Log.d(TAG, "✅ Đã lưu địa chỉ: $address")
                Toast.makeText(this@MapboxPickerActivity, "✅ Đã lưu địa chỉ giao hàng!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi lưu Firestore: ${e.message}")
                Toast.makeText(this@MapboxPickerActivity, "Lỗi lưu địa chỉ: ${e.message}", Toast.LENGTH_LONG).show()
                binding.confirmAddressBtn.isEnabled = true
            } finally {
                binding.geocodeProgressBar.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────

    private fun hideSuggestions() {
        searchResults = emptyList()
        suggestionsAdapter.clear()
        binding.suggestionsCard.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchAddressEditText.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        geocodeJob?.cancel()
        searchJob?.cancel()
    }

    companion object {
        private const val TAG = "MapboxPickerActivity"
    }
}
