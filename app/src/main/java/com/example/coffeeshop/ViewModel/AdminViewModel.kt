package com.example.coffeeshop.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.Repository.AdminRepository
import com.example.coffeeshop.Repository.CloudinaryRepository
import kotlinx.coroutines.launch
import java.io.File

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val adminRepo = AdminRepository()
    private val cloudinaryRepo = CloudinaryRepository()

    // ── Loading / Error state ────────────────────────────────────
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun clearToast() { _toastMessage.value = null }

    // ── Categories ───────────────────────────────────────────────
    private val _categories = MutableLiveData<List<CategoryModel>>()
    val categories: LiveData<List<CategoryModel>> = _categories

    fun loadCategories() {
        viewModelScope.launch {
            _loading.value = true
            _categories.value = adminRepo.getAllCategories()
            _loading.value = false
        }
    }

    fun addCategory(title: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.addCategory(title)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Thêm danh mục thành công!"
                loadCategories()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun updateCategory(id: Int, title: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.updateCategory(id, title)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Cập nhật danh mục thành công!"
                loadCategories()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun softDeleteCategory(id: Int) {
        viewModelScope.launch {
            val result = adminRepo.softDeleteCategory(id)
            if (result.isSuccess) {
                _toastMessage.value = "Đã ẩn danh mục"
                loadCategories()
            } else {
                _toastMessage.value = "❌ Ẩn thất bại"
            }
        }
    }

    fun restoreCategory(id: Int) {
        viewModelScope.launch {
            val result = adminRepo.restoreCategory(id)
            if (result.isSuccess) {
                _toastMessage.value = "Đã khôi phục danh mục"
                loadCategories()
            } else {
                _toastMessage.value = "❌ Khôi phục thất bại"
            }
        }
    }

    // ── Items ────────────────────────────────────────────────────
    private val _items = MutableLiveData<List<ItemsModel>>()
    val items: LiveData<List<ItemsModel>> = _items

    fun loadItems() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = adminRepo.getAllItems()
            _loading.value = false
        }
    }

    fun addItem(item: ItemsModel) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.addItem(item)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Thêm sản phẩm thành công!"
                loadItems()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun updateItem(itemKey: String, item: ItemsModel) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.updateItem(itemKey, item)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Cập nhật sản phẩm thành công!"
                loadItems()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun softDeleteItem(itemKey: String) {
        viewModelScope.launch {
            val result = adminRepo.softDeleteItem(itemKey)
            if (result.isSuccess) {
                _toastMessage.value = "Đã ẩn sản phẩm"
                loadItems()
            } else {
                _toastMessage.value = "❌ Ẩn thất bại: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun restoreItem(itemKey: String) {
        viewModelScope.launch {
            val result = adminRepo.restoreItem(itemKey)
            if (result.isSuccess) {
                _toastMessage.value = "Đã khôi phục sản phẩm"
                loadItems()
            } else {
                _toastMessage.value = "❌ Khôi phục thất bại: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    // ── Banners ──────────────────────────────────────────────────
    private val _banners = MutableLiveData<List<BannerModel>>()
    val banners: LiveData<List<BannerModel>> = _banners

    fun loadBanners() {
        viewModelScope.launch {
            _loading.value = true
            _banners.value = adminRepo.getAllBanners()
            _loading.value = false
        }
    }

    fun addBanner(url: String, title: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.addBanner(url, title)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Thêm banner thành công!"
                loadBanners()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun updateBanner(index: Int, url: String, title: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = adminRepo.updateBanner(index, url, title)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Cập nhật banner thành công!"
                loadBanners()
            } else {
                _toastMessage.value = "❌ Lỗi: ${result.exceptionOrNull()?.message}"
            }
            _loading.value = false
        }
    }

    fun softDeleteBanner(index: Int) {
        viewModelScope.launch {
            val result = adminRepo.softDeleteBanner(index)
            if (result.isSuccess) {
                _toastMessage.value = "Đã ẩn banner"
                loadBanners()
            } else {
                _toastMessage.value = "❌ Ẩn thất bại"
            }
        }
    }

    fun restoreBanner(index: Int) {
        viewModelScope.launch {
            val result = adminRepo.restoreBanner(index)
            if (result.isSuccess) {
                _toastMessage.value = "Đã khôi phục banner"
                loadBanners()
            } else {
                _toastMessage.value = "❌ Khôi phục thất bại"
            }
        }
    }

    // ── Orders ───────────────────────────────────────────────────
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    fun loadOrders() {
        viewModelScope.launch {
            _loading.value = true
            _orders.value = adminRepo.getAllOrders()
            _loading.value = false
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val result = adminRepo.updateOrderStatus(orderId, newStatus)
            if (result.isSuccess) {
                _toastMessage.value = "✅ Đã cập nhật: ${Order.getStatusLabel(newStatus)}"
                loadOrders()
            } else {
                _toastMessage.value = "❌ Cập nhật thất bại"
            }
        }
    }

    // ── Cloudinary Upload ────────────────────────────────────────
    private val _uploadedUrl = MutableLiveData<String?>()
    val uploadedUrl: LiveData<String?> = _uploadedUrl

    private val _uploading = MutableLiveData(false)
    val uploading: LiveData<Boolean> = _uploading

    fun uploadImage(imageFile: File) {
        viewModelScope.launch {
            _uploading.value = true
            val result = cloudinaryRepo.uploadImage(imageFile)
            if (result.isSuccess) {
                _uploadedUrl.value = result.getOrNull()
                _toastMessage.value = "✅ Upload ảnh thành công!"
            } else {
                _toastMessage.value = "❌ Upload ảnh thất bại: ${result.exceptionOrNull()?.message}"
            }
            _uploading.value = false
        }
    }

    fun clearUploadedUrl() {
        _uploadedUrl.value = null
    }
}
