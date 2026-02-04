package com.example.coffeeshop.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Repository.MainRepository

class MainViewModel:ViewModel (){
    private val repository = MainRepository()

    fun loadBanner():MutableLiveData<MutableList<BannerModel>>{
        return repository.loadBanner()
    }

    fun loadCategory():MutableLiveData<MutableList<CategoryModel>>{
        return repository.loadCategory()
    }

    fun loadAllItems(): MutableLiveData<MutableList<ItemsModel>> {
        return repository.loadAllItems()
    }

    fun loadPopular():MutableLiveData<MutableList<ItemsModel>>{
        return repository.loadPopular()
    }

    fun loadItemCategory(categoryId:String):MutableLiveData<MutableList<ItemsModel>>{
        return repository.loadItemCategory(categoryId)
    }

    fun searchItems(query: String): MutableLiveData<MutableList<ItemsModel>> {
        return repository.searchItems(query)
    }
}
