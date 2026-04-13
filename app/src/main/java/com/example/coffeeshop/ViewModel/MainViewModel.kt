package com.example.coffeeshop.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.FilterOptions
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Repository.MainRepository

class MainViewModel : ViewModel() {
    private val repository = MainRepository()

    fun loadBanner(): MutableLiveData<MutableList<BannerModel>> {
        return repository.loadBanner()
    }

    fun loadCategory(): MutableLiveData<MutableList<CategoryModel>> {
        return repository.loadCategory()
    }

    fun loadAllItems(): MutableLiveData<MutableList<ItemsModel>> {
        return repository.loadAllItems()
    }

    fun loadPopular(): MutableLiveData<MutableList<ItemsModel>> {
        return repository.loadPopular()
    }

    fun loadItemCategory(categoryId: String): MutableLiveData<MutableList<ItemsModel>> {
        return repository.loadItemCategory(categoryId)
    }

    fun searchItems(query: String): MutableLiveData<MutableList<ItemsModel>> {
        return repository.searchItems(query)
    }

    fun loadAllItemsAndPopular(): MutableLiveData<MutableList<ItemsModel>> {
        return repository.loadAllItemsAndPopular()
    }

    /**
     * Áp dụng FilterOptions lên danh sách items đã có sẵn (in-memory).
     * Không cần request Firebase thêm lần nào nữa.
     */
    fun applyFilter(items: List<ItemsModel>, options: FilterOptions): List<ItemsModel> {
        var result = items.toMutableList()

        // 1. Lọc theo category
        if (options.categoryId.isNotEmpty()) {
            result = result.filter { it.categoryId == options.categoryId }.toMutableList()
        }

        // 2. Lọc theo khoảng giá
        if (options.minPrice > 0 || options.maxPrice < Double.MAX_VALUE) {
            result = result.filter {
                it.price >= options.minPrice && it.price <= options.maxPrice
            }.toMutableList()
        }

        // 3. Sắp xếp
        result = when (options.sortBy) {
            FilterOptions.SortBy.PRICE_ASC   -> result.sortedBy { it.price }.toMutableList()
            FilterOptions.SortBy.PRICE_DESC  -> result.sortedByDescending { it.price }.toMutableList()
            FilterOptions.SortBy.RATING_DESC -> result.sortedByDescending { it.rating }.toMutableList()
            FilterOptions.SortBy.DEFAULT     -> result
        }

        return result
    }
}
